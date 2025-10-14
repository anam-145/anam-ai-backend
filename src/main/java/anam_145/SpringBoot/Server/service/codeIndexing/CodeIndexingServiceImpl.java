package anam_145.SpringBoot.Server.service.codeIndexing;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import anam_145.SpringBoot.Server.domain.aiGuide.MiniAppCodeIndex;
import anam_145.SpringBoot.Server.domain.aiGuide.ScreenInfo;
import anam_145.SpringBoot.Server.repository.ComposableInfoRepository;
import anam_145.SpringBoot.Server.repository.MiniAppCodeIndexRepository;
import anam_145.SpringBoot.Server.repository.ScreenInfoRepository;
import anam_145.SpringBoot.Server.service.kotlinASTParser.KotlinASTParser;
import anam_145.SpringBoot.Server.service.zipExtractorService.ZipExtractorService;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.KotlinFileContentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 코드 인덱싱 서비스 구현체
 * ZIP 파일 업로드부터 DB 저장까지 전체 플로우를 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeIndexingServiceImpl implements CodeIndexingService {

    private final ZipExtractorService zipExtractorService;
    private final KotlinASTParser kotlinASTParser;
    private final MiniAppCodeIndexRepository miniAppCodeIndexRepository;
    private final ScreenInfoRepository screenInfoRepository;
    private final ComposableInfoRepository composableInfoRepository;

    @Override
    @Transactional // 전체 프로세스를 하나의 트랜잭션으로 묶어 일관성 보장
    public int indexMiniAppCode(String appId, MultipartFile zipFile) {
        log.info("MiniApp 코드 인덱싱 시작: appId={}", appId);

        // 1. 기존 인덱스 데이터 삭제 (재인덱싱 시)
        // 이미 등록된 모듈앱을 다시 업로드하면 기존 데이터를 지우고 새로 분석
        deleteExistingIndex(appId);

        // 2. ZIP 파일에서 Kotlin 소스 파일 추출
        // ZipExtractorService가 .kt 파일만 필터링하여 추출
        List<KotlinFileContentDTO> kotlinFiles = zipExtractorService.extractKotlinFiles(zipFile);
        log.info("추출된 Kotlin 파일 개수: {}", kotlinFiles.size());

        // 3. 각 Kotlin 파일을 AST 파싱하여 UI 요소 추출
        List<ComposableInfo> allComposables = new ArrayList<>();
        for (KotlinFileContentDTO file : kotlinFiles) {
            try {
                // 파일별로 AST 파싱 수행
                // @Composable 함수를 찾아서 내부의 Button, Text 등 UI 요소 정보 추출
                List<ComposableInfo> composables = kotlinASTParser.parseKotlinFile(
                    appId,
                    file.getFileName(),
                    file.getContent()
                );
                allComposables.addAll(composables);
            } catch (Exception e) {
                // 특정 파일 파싱 실패해도 전체 프로세스는 계속 진행
                // 일부 파일에 문법 오류가 있어도 나머지 파일은 처리
                log.warn("파일 파싱 실패 (계속 진행): fileName={}", file.getFileName(), e);
            }
        }

        log.info("총 추출된 UI 요소 개수: {}", allComposables.size());

        // 4. 화면별로 그룹화 (ScreenInfo 생성 준비)
        // 예: TransferScreen.kt의 UI 요소들을 "TransferScreen" 화면으로 그룹화
        Map<String, List<ComposableInfo>> composablesByScreen = groupComposablesByScreen(allComposables);

        // 5. MiniAppCodeIndex 생성 (최상위 인덱스 엔티티)
        MiniAppCodeIndex codeIndex = MiniAppCodeIndex.builder()
            .appId(appId)
            .build();
        miniAppCodeIndexRepository.save(codeIndex); // indexedAt은 @CreationTimestamp로 자동 설정

        // 6. 화면별로 ScreenInfo와 ComposableInfo 저장
        int totalSaved = 0;
        for (Map.Entry<String, List<ComposableInfo>> entry : composablesByScreen.entrySet()) {
            String screenName = entry.getKey();
            List<ComposableInfo> composables = entry.getValue();

            // ScreenInfo 생성 (화면 단위)
            ScreenInfo screenInfo = ScreenInfo.builder()
                .appId(appId)
                .name(screenName) // 예: "TransferScreen"
                .sourceFile(getSourceFileForScreen(composables)) // 예: "app/.../TransferScreen.kt"
                .build();

            // MiniAppCodeIndex와 양방향 관계 설정
            codeIndex.addScreen(screenInfo);

            // ScreenInfo 저장
            screenInfoRepository.save(screenInfo);

            // 각 ComposableInfo에 ScreenInfo 설정 후 저장
            for (ComposableInfo composable : composables) {
                composable.setScreenInfo(screenInfo); // 양방향 관계 설정 (screenId도 자동 설정됨)
            }

            // ComposableInfo 일괄 저장 (성능 최적화)
            composableInfoRepository.saveAll(composables);

            totalSaved += composables.size();
            log.debug("화면 저장 완료: screenName={}, UI 요소 개수={}", screenName, composables.size());
        }

        log.info("MiniApp 코드 인덱싱 완료: appId={}, 총 {}개 UI 요소 저장", appId, totalSaved);
        return totalSaved;
    }

    /**
     * 기존 인덱스 데이터 삭제 (재인덱싱 시)
     */
    private void deleteExistingIndex(String appId) {
        // MiniAppCodeIndex가 존재하면 Cascade로 연관된 데이터 모두 삭제
        // CascadeType.ALL 설정으로 ScreenInfo → ComposableInfo까지 자동 삭제
        miniAppCodeIndexRepository.findById(appId).ifPresent(existingIndex -> {
            log.info("기존 인덱스 삭제: appId={}", appId);
            miniAppCodeIndexRepository.delete(existingIndex);
        });
    }

    /**
     * ComposableInfo 리스트를 화면명(Screen)별로 그룹화
     * 파일명에서 화면명을 추출한다 (예: TransferScreen.kt -> TransferScreen)
     */
    private Map<String, List<ComposableInfo>> groupComposablesByScreen(List<ComposableInfo> composables) {
        return composables.stream()
            .collect(Collectors.groupingBy(composable -> {
                // 파일명에서 화면명 추출
                String fileName = composable.getSourceFile();
                if (fileName == null) {
                    return "Unknown";
                }

                // 경로에서 파일명만 추출 (예: app/src/main/.../TransferScreen.kt -> TransferScreen.kt)
                String fileNameOnly = fileName.contains("/")
                    ? fileName.substring(fileName.lastIndexOf("/") + 1)
                    : fileName;

                // 확장자 제거 (TransferScreen.kt -> TransferScreen)
                return fileNameOnly.replace(".kt", "");
            }));
    }

    /**
     * 화면의 소스 파일 경로 추출
     * 같은 화면의 UI 요소들은 모두 같은 파일에서 나오므로 첫 번째 요소의 경로 사용
     */
    private String getSourceFileForScreen(List<ComposableInfo> composables) {
        return composables.isEmpty() ? null : composables.get(0).getSourceFile();
    }
}
