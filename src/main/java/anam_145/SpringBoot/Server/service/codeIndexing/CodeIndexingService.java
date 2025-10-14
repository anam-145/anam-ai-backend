package anam_145.SpringBoot.Server.service.codeIndexing;

import org.springframework.web.multipart.MultipartFile;

/**
 * MiniApp 코드 인덱싱 서비스
 * ZIP 추출 → AST 파싱 → DB 저장까지의 전체 플로우를 관리한다.
 */
public interface CodeIndexingService {

    /**
     * MiniApp의 Kotlin 소스 코드를 분석하고 인덱싱한다.
     *
     * @param appId MiniApp ID
     * @param zipFile 업로드된 ZIP 파일
     * @return 인덱싱된 UI 요소 개수
     */
    int indexMiniAppCode(String appId, MultipartFile zipFile);
}
