package anam_145.SpringBoot.Server.web.controller;

import anam_145.SpringBoot.Server.apiPayload.ApiResponse;
import anam_145.SpringBoot.Server.service.codeIndexing.CodeIndexingService;
import anam_145.SpringBoot.Server.service.zipExtractorService.ZipExtractorService;
import anam_145.SpringBoot.Server.web.controller.specification.AnalyzeSpecification;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.KotlinFileContentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analyze")
public class AnalyzeController implements AnalyzeSpecification {

    private final ZipExtractorService zipExtractorService; // ZIP 파일 추출 서비스 로직
    private final CodeIndexingService codeIndexingService; // 코드 인덱싱 서비스 로직

    /**
     * MiniApp ZIP 파일에서 Kotlin 소스 파일 추출 API
     * POST /api/v1/analyze/kotlin-files
     *
     * @param zipFile 업로드된 ZIP 파일 (multipart/form-data)
     * @return 추출된 Kotlin 파일 목록
     */
    @Override
    @PostMapping(value = "/kotlin/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<KotlinFileContentDTO>> extractKotlinFiles(
            @RequestPart("file") MultipartFile zipFile) { // form-data의 "file" 필드로 전송

        log.info("Received ZIP file upload request: {} ({}bytes)",
                zipFile.getOriginalFilename(), zipFile.getSize());

        List<KotlinFileContentDTO> kotlinFiles = zipExtractorService.extractKotlinFiles(zipFile); // 서비스 계층 호출

        log.info("Successfully extracted {} Kotlin files", kotlinFiles.size());
        return ApiResponse.onSuccess(kotlinFiles); // 공통 응답 형식으로 래핑하여 반환
    }

    /**
     * MiniApp 등록 및 코드 인덱싱 API
     * POST /api/v1/analyze/miniapp/register
     *
     * MiniApp ZIP 파일을 업로드하면 자동으로:
     * 1. ZIP에서 Kotlin 파일 추출
     * 2. AST 파싱하여 UI 요소 추출
     * 3. DB에 저장
     *
     * @param appId MiniApp 고유 ID
     * @param zipFile MiniApp 프로젝트 ZIP 파일
     * @return 인덱싱된 UI 요소 개수
     */
    @PostMapping(value = "/miniapp/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Integer> registerMiniApp(
            @RequestParam("appId") String appId, // Query Parameter로 appId 전달
            @RequestPart("zipFile") MultipartFile zipFile) { // form-data의 "zipFile" 필드로 전송

        log.info("MiniApp 등록 요청: appId={}, fileName={} ({}bytes)",
                appId, zipFile.getOriginalFilename(), zipFile.getSize());

        // 코드 인덱싱 서비스 호출 (ZIP 추출 → AST 파싱 → DB 저장)
        int indexedCount = codeIndexingService.indexMiniAppCode(appId, zipFile);

        log.info("MiniApp 등록 완료: appId={}, 인덱싱된 UI 요소 개수={}", appId, indexedCount);
        return ApiResponse.onSuccess(indexedCount);
    }
}
