package anam_145.SpringBoot.Server.service.kotlinASTParser;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;

import java.util.List;

/**
 * Kotlin 소스 코드를 AST로 파싱하여 Jetpack Compose UI 요소를 추출하는 서비스
 */
public interface KotlinASTParser {

    /**
     * Kotlin 소스 코드 파일을 파싱하여 UI 요소 정보를 추출한다.
     *
     * @param appId MiniApp ID
     * @param fileName 소스 파일 이름 (예: "TransferScreen.kt")
     * @param sourceCode Kotlin 소스 코드 문자열
     * @return 추출된 UI 요소 정보 목록
     */
    List<ComposableInfo> parseKotlinFile(String appId, String fileName, String sourceCode);
}
