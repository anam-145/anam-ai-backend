package anam_145.SpringBoot.Server.service.zipExtractorService;

import anam_145.SpringBoot.Server.apiPayload.code.status.error.CommonErrorStatus;
import anam_145.SpringBoot.Server.apiPayload.exception.InvalidZipFileException;
import anam_145.SpringBoot.Server.apiPayload.exception.ZipExtractionException;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.KotlinFileContentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipExtractorServiceImpl implements ZipExtractorService {

    private static final long MAX_ZIP_SIZE = 50 * 1024 * 1024; // 최대 ZIP 파일 크기 (50MB), DoS 공격 방지용
    private static final String KOTLIN_EXTENSION = ".kt"; // Kotlin 소스 파일 확장자

    /**
     * ZIP 파일에서 모든 Kotlin 소스 파일을 추출하는 메인 메서드
     * MiniApp 승인 시 업로드된 ZIP 파일을 분석하여 .kt 파일만 추출한다.
     * 추출된 파일은 이후 KotlinASTParser로 전달되어 UI 요소 정보로 변환된다.
     *
     * @param zipFile 업로드된 ZIP 파일
     * @return 추출된 Kotlin 파일 목록
     * @throws InvalidZipFileException 유효하지 않은 ZIP 파일인 경우
     * @throws ZipExtractionException ZIP 파일 처리 중 오류 발생 시
     */
    @Override
    public List<KotlinFileContentDTO> extractKotlinFiles(MultipartFile zipFile) {
        validateZipFile(zipFile); // 1단계: ZIP 파일 유효성 검사 (크기, 형식 등)

        List<KotlinFileContentDTO> kotlinFiles = new ArrayList<>();

        // 2단계: ZIP 파일을 열고 내부 엔트리들을 순회하며 처리
        try (InputStream inputStream = zipFile.getInputStream(); // MultipartFile을 InputStream으로 변환
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) { // UTF-8로 ZIP 읽기

            ZipEntry entry; // ZIP 내부의 각 파일/디렉토리를 나타내는 객체
            while ((entry = zipInputStream.getNextEntry()) != null) { // 모든 엔트리를 순회

                if (shouldProcessEntry(entry)) { // 3단계: .kt 파일인지, 빌드 산출물이 아닌지 등을 체크
                    String content = readFileContent(zipInputStream); // 4단계: 파일 내용을 문자열로 읽기

                    // 5단계: DTO 객체로 변환하여 리스트에 추가
                    kotlinFiles.add(KotlinFileContentDTO.builder()
                            .fileName(entry.getName()) // 파일 경로 (예: app/src/main/kotlin/TransferScreen.kt)
                            .content(content) // 파일 내용 (Kotlin 소스 코드)
                            .fileSize(content.getBytes(StandardCharsets.UTF_8).length) // 바이트 크기 계산
                            .build());

                    log.debug("Extracted Kotlin file: {} ({}bytes)", entry.getName(), content.length());
                }

                zipInputStream.closeEntry(); // 현재 엔트리 처리 완료
            }

        } catch (IOException e) { // ZIP 읽기 실패, 손상된 파일 등의 경우
            log.error("Failed to extract ZIP file: {}", zipFile.getOriginalFilename(), e);
            throw new ZipExtractionException(CommonErrorStatus.ZIP_EXTRACTION_FAILED);
        }

        if (kotlinFiles.isEmpty()) { // ZIP 내에 Kotlin 파일이 하나도 없는 경우
            log.warn("No Kotlin files found in ZIP: {}", zipFile.getOriginalFilename());
            throw new ZipExtractionException(CommonErrorStatus.NO_KOTLIN_FILES_FOUND);
        }

        log.info("Successfully extracted {} Kotlin files from ZIP: {}",
                kotlinFiles.size(), zipFile.getOriginalFilename());
        return kotlinFiles; // 추출된 모든 Kotlin 파일 정보 반환
    }

    /**
     * ZIP 엔트리가 처리 대상인지 판단하는 필터 메서드
     * Kotlin 소스 파일만 추출하고, 빌드 산출물과 보안 위협 파일은 제외한다.
     *
     * @param entry ZIP 파일 내부의 엔트리 (파일 또는 디렉토리)
     * @return 처리 대상이면 true, 제외 대상이면 false
     */
    private boolean shouldProcessEntry(ZipEntry entry) {
        if (entry.isDirectory()) // 디렉토리는 처리 대상이 아님 (파일만 추출)
            return false;

        String name = entry.getName(); // 엔트리의 전체 경로 (예: app/src/main/kotlin/TransferScreen.kt)

        if (!name.endsWith(KOTLIN_EXTENSION)) // .kt 확장자가 아니면 제외 (Kotlin 파일만 처리)
            return false;

        if (name.contains("/build/") || name.contains("/.gradle/")) { // 빌드 산출물 디렉토리 체크
            log.debug("Skipping build artifact: {}", name); // 컴파일된 .class 파일, 빌드 캐시 등은 제외
            return false;
        }

        if (name.contains("..")) { // 상위 디렉토리 탐색 시도 감지 (Zip Slip 공격 방어)
            log.warn("Suspicious file path detected (Zip Slip attempt): {}", name); // 보안 위협 로깅
            return false; // ../../etc/passwd 같은 경로 차단
        }

        return true; // 모든 필터를 통과한 정상적인 Kotlin 소스 파일
    }

    /**
     * 업로드된 ZIP 파일의 유효성을 검사하는 메서드
     * DoS 공격 방지를 위한 크기 제한, MIME 타입 검증 등을 수행한다.
     *
     * @param zipFile 검증할 MultipartFile 객체
     * @throws InvalidZipFileException 파일이 유효하지 않은 경우
     */
    private void validateZipFile(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) { // null 체크 및 0바이트 파일 방어
            log.error("Uploaded ZIP file is null or empty"); // 파일이 전송되지 않았거나 비어있는 경우
            throw new InvalidZipFileException(CommonErrorStatus.ZIP_FILE_EMPTY);
        }

        if (zipFile.getSize() > MAX_ZIP_SIZE) { // 50MB 크기 제한 체크 (DoS 공격 방지)
            log.error("ZIP file size exceeds limit: {}bytes (max: {}bytes)",
                    zipFile.getSize(), MAX_ZIP_SIZE); // 실제 크기와 제한값 로깅
            throw new InvalidZipFileException(CommonErrorStatus.ZIP_FILE_TOO_LARGE); // 초과 시 거부
        }

        String contentType = zipFile.getContentType(); // HTTP 헤더의 Content-Type 추출
        if (contentType == null || // Content-Type이 없거나
                (!contentType.equals("application/zip") && // 표준 ZIP MIME 타입이 아니고
                        !contentType.equals("application/x-zip-compressed") && // 구형 ZIP MIME 타입도 아니고
                        !contentType.equals("application/octet-stream"))) { // 바이너리 스트림도 아닌 경우
            log.error("Invalid content type: {}", contentType); // 잘못된 파일 형식 로깅
            throw new InvalidZipFileException(CommonErrorStatus.ZIP_FILE_INVALID_FORMAT); // ZIP이 아닌 파일 거부
        }

        log.debug("ZIP file validation passed: {} ({}bytes)",
                zipFile.getOriginalFilename(), zipFile.getSize()); // 검증 통과 로깅
    }

    /**
     * ZIP 엔트리의 파일 내용을 문자열로 읽어오는 메서드
     * UTF-8 인코딩을 사용하여 한글이 포함된 Kotlin 소스 코드도 정상 처리한다.
     *
     * @param inputStream ZIP 엔트리의 입력 스트림 (zipInputStream.getNextEntry() 이후 상태)
     * @return UTF-8로 디코딩된 파일 내용 전체
     * @throws IOException 파일 읽기 실패 시
     */
    private String readFileContent(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder(); // 파일 내용을 누적할 버퍼

        try (BufferedReader reader = new BufferedReader( // 버퍼링을 통한 효율적인 읽기
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) { // UTF-8 인코딩 명시 (한글 주석, 문자열 처리)

            String line; // 한 줄씩 읽어온 텍스트
            while ((line = reader.readLine()) != null) { // 파일 끝까지 반복
                content.append(line).append("\n"); // 줄바꿈 문자 추가 (readLine은 줄바꿈 제거하므로 복원)
            }
        } // try-with-resources로 자동 리소스 해제 (reader 자동 close)

        return content.toString(); // 누적된 전체 파일 내용 반환
    }
}
