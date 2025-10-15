package anam_145.SpringBoot.Server.service.htmlParser;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;

import java.util.List;

/**
 * HTML 파일을 파싱하여 UI 요소를 추출하는 서비스
 */
public interface HTMLParser {

    /**
     * HTML 파일을 파싱하여 UI 요소 정보를 추출한다.
     *
     * @param appId MiniApp ID
     * @param fileName 파일 이름 (예: "pages/index/index.html")
     * @param htmlContent HTML 소스 코드 문자열
     * @return 추출된 UI 요소 정보 목록
     */
    List<ComposableInfo> parseHtmlFile(String appId, String fileName, String htmlContent);
}
