package anam_145.SpringBoot.Server.service.kotlinASTParser;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Kotlin AST 파서 구현체
 * kotlin-compiler-embeddable을 사용하여 Jetpack Compose UI 요소를 추출한다.
 */
@Slf4j
@Service
public class KotlinASTParserImpl implements KotlinASTParser {

    @Override
    public List<ComposableInfo> parseKotlinFile(String appId, String fileName, String sourceCode) {
        log.info("Kotlin 파일 파싱 시작: appId={}, fileName={}", appId, fileName);

        List<ComposableInfo> composableInfoList = new ArrayList<>();

        try {
            // 1. Kotlin 컴파일러 환경 설정
            // Disposer: 파싱이 끝난 후 리소스를 정리하기 위한 객체
            var disposer = Disposer.newDisposable();

            try {
                // CompilerConfiguration: Kotlin 컴파일러의 설정을 담는 객체
                CompilerConfiguration configuration = new CompilerConfiguration();

                // KotlinCoreEnvironment: Kotlin 코드를 파싱하기 위한 핵심 환경
                // EnvironmentConfigFiles.JVM_CONFIG_FILES: JVM용 Kotlin 파일 설정 사용
                KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
                    disposer,
                    configuration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
                );

                // 2. Kotlin 소스 코드를 PSI(Program Structure Interface) 파일로 변환
                // PSI는 IntelliJ 플랫폼의 코드 구조 표현 방식
                KtPsiFactory psiFactory = new KtPsiFactory(environment.getProject());
                KtFile ktFile = psiFactory.createFile(fileName, sourceCode);

                log.debug("PSI 파일 생성 완료: {}", fileName);

                // 3. AST를 순회하며 @Composable 함수 찾기
                // ktFile의 모든 선언(declaration)을 순회
                for (KtDeclaration declaration : ktFile.getDeclarations()) {
                    // 함수 선언인지 확인
                    if (declaration instanceof KtNamedFunction) {
                        KtNamedFunction function = (KtNamedFunction) declaration;

                        // @Composable 어노테이션이 붙은 함수만 처리
                        if (hasComposableAnnotation(function)) {
                            String functionName = function.getName();
                            log.debug("@Composable 함수 발견: {}", functionName);

                            // 4. 함수 내부의 UI 요소 추출
                            List<ComposableInfo> extracted = extractComposablesFromFunction(
                                appId,
                                fileName,
                                functionName,
                                function
                            );

                            composableInfoList.addAll(extracted);
                        }
                    }
                }

                log.info("파싱 완료: 총 {}개의 UI 요소 추출", composableInfoList.size());

            } finally {
                // 반드시 리소스를 해제하여 메모리 누수 방지
                Disposer.dispose(disposer);
            }

        } catch (Exception e) {
            log.error("Kotlin 파싱 중 오류 발생: fileName={}", fileName, e);
            // 파싱 실패 시 빈 리스트 반환 (일부 파일 실패해도 전체 프로세스는 계속 진행)
        }

        return composableInfoList;
    }

    /**
     * 함수에 @Composable 어노테이션이 있는지 확인
     */
    private boolean hasComposableAnnotation(KtNamedFunction function) {
        // 함수의 모든 어노테이션 엔트리를 확인
        for (KtAnnotationEntry annotation : function.getAnnotationEntries()) {
            String annotationText = annotation.getShortName() != null
                ? annotation.getShortName().asString()
                : "";

            // "Composable" 어노테이션 찾기
            if ("Composable".equals(annotationText)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @Composable 함수 내부에서 UI 요소를 추출
     */
    private List<ComposableInfo> extractComposablesFromFunction(
        String appId,
        String fileName,
        String functionName,
        KtNamedFunction function
    ) {
        List<ComposableInfo> result = new ArrayList<>();

        // 함수의 body(본문)가 없으면 추출할 게 없음
        KtExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) {
            return result;
        }

        // 함수 본문의 모든 하위 요소를 재귀적으로 방문
        bodyExpression.accept(new KtTreeVisitorVoid() {
            @Override
            public void visitCallExpression(KtCallExpression expression) {
                // CallExpression: 함수 호출을 나타냄 (예: Button(...), Text(...))
                super.visitCallExpression(expression);

                // 호출된 함수의 이름 추출
                String callName = extractCallName(expression);

                if (callName != null && isUIComponent(callName)) {
                    // UI 컴포넌트인 경우 정보 추출
                    log.debug("UI 컴포넌트 발견: {} in {}", callName, functionName);

                    ComposableInfo info = buildComposableInfo(
                        appId,
                        fileName,
                        functionName,
                        callName,
                        expression
                    );

                    result.add(info);
                }
            }
        });

        return result;
    }

    /**
     * CallExpression에서 호출된 함수 이름 추출
     */
    private String extractCallName(KtCallExpression expression) {
        // calleeExpression: 호출되는 대상 (예: Button, Text 등)
        KtExpression calleeExpression = expression.getCalleeExpression();

        if (calleeExpression instanceof KtNameReferenceExpression) {
            // 단순 이름 참조인 경우 (예: Button)
            KtNameReferenceExpression nameRef = (KtNameReferenceExpression) calleeExpression;
            return nameRef.getReferencedName();
        }

        return null;
    }

    /**
     * 주요 Jetpack Compose UI 컴포넌트인지 확인
     */
    private boolean isUIComponent(String name) {
        // 주로 사용되는 Compose UI 요소들
        return name.equals("Button") ||
               name.equals("Text") ||
               name.equals("TextField") ||
               name.equals("OutlinedTextField") ||
               name.equals("Icon") ||
               name.equals("Image") ||
               name.equals("Card") ||
               name.equals("LazyColumn") ||
               name.equals("LazyRow") ||
               name.equals("Column") ||
               name.equals("Row") ||
               name.equals("Box") ||
               name.equals("IconButton") ||
               name.equals("FloatingActionButton") ||
               name.equals("Checkbox") ||
               name.equals("RadioButton") ||
               name.equals("Switch") ||
               name.equals("Slider");
    }

    /**
     * ComposableInfo 엔티티 생성
     */
    private ComposableInfo buildComposableInfo(
        String appId,
        String fileName,
        String screenName,
        String componentType,
        KtCallExpression expression
    ) {
        // 1. text 파라미터 추출 (Button, Text 등에 표시되는 텍스트)
        String displayText = extractTextParameter(expression);

        // 2. onClick 파라미터 추출 (버튼 클릭 시 동작)
        String onClickCode = extractOnClickParameter(expression);

        // 3. modifier 파라미터 추출 (레이아웃 정보, testTag 등)
        String modifierCode = extractModifierParameter(expression);

        // 4. testTag 또는 contentDescription 추출 (UI 요소 식별자)
        String composableId = extractComposableId(modifierCode);

        // 5. semantics의 contentDescription 추출 (접근성 힌트)
        String semanticHint = extractSemanticHint(modifierCode);

        // 6. 소스 코드 위치 정보
        int lineNumber = getLineNumber(expression);

        // ComposableInfo 엔티티 빌드
        return ComposableInfo.builder()
            .appId(appId)
            .type(componentType)
            .composableId(composableId)
            .text(displayText)
            .semanticHint(semanticHint)
            .onClickCode(onClickCode)
            .modifierCode(modifierCode)
            .sourceFile(fileName)
            .lineNumber(lineNumber)
            .build();
    }

    /**
     * text 파라미터 값 추출
     * 예: Text("로그인") -> "로그인"
     * 예: Button(...) { Text("확인") } -> "확인"
     */
    private String extractTextParameter(KtCallExpression expression) {
        // 1. 직접 전달된 문자열 인자 확인 (예: Text("로그인"))
        List<? extends ValueArgument> arguments = expression.getValueArguments();
        for (ValueArgument arg : arguments) {
            KtExpression argExpr = arg.getArgumentExpression();
            if (argExpr instanceof KtStringTemplateExpression) {
                // 문자열 리터럴인 경우
                return extractStringLiteral((KtStringTemplateExpression) argExpr);
            }
        }

        // 2. 람다 블록 내부의 Text 호출 확인 (예: Button { Text("확인") })
        for (KtLambdaArgument lambdaArg : expression.getLambdaArguments()) {
            KtLambdaExpression lambda = lambdaArg.getLambdaExpression();
            KtExpression bodyExpr = lambda.getBodyExpression();

            if (bodyExpr != null) {
                // 람다 내부에서 Text 호출을 찾아 텍스트 추출
                String textFromLambda = findTextInLambda(bodyExpr);
                if (textFromLambda != null) {
                    return textFromLambda;
                }
            }
        }

        return null;
    }

    /**
     * 람다 본문에서 Text 컴포넌트의 텍스트 추출
     */
    private String findTextInLambda(KtExpression bodyExpr) {
        // 람다 내부의 모든 CallExpression을 순회
        for (PsiElement child : bodyExpr.getChildren()) {
            if (child instanceof KtCallExpression) {
                KtCallExpression call = (KtCallExpression) child;
                String callName = extractCallName(call);

                // Text 호출을 찾음
                if ("Text".equals(callName)) {
                    // Text의 첫 번째 인자(문자열)를 반환
                    List<? extends ValueArgument> textArgs = call.getValueArguments();
                    if (!textArgs.isEmpty()) {
                        KtExpression argExpr = textArgs.get(0).getArgumentExpression();
                        if (argExpr instanceof KtStringTemplateExpression) {
                            return extractStringLiteral((KtStringTemplateExpression) argExpr);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 문자열 리터럴 값 추출
     * 예: "로그인" -> 로그인
     */
    private String extractStringLiteral(KtStringTemplateExpression stringExpr) {
        // 문자열 템플릿의 모든 엔트리를 결합
        StringBuilder sb = new StringBuilder();
        for (KtStringTemplateEntry entry : stringExpr.getEntries()) {
            // 단순 텍스트인 경우 (변수 보간이 아닌 경우)
            if (entry instanceof KtLiteralStringTemplateEntry) {
                sb.append(entry.getText());
            } else if (entry instanceof KtEscapeStringTemplateEntry) {
                // 이스케이프 문자 처리 (예: \n)
                sb.append(entry.getText());
            }
        }
        return sb.toString();
    }

    /**
     * onClick 파라미터 코드 추출
     * 예: onClick = { viewModel.transfer() } -> "{ viewModel.transfer() }"
     */
    private String extractOnClickParameter(KtCallExpression expression) {
        // 모든 value argument를 순회
        for (ValueArgument arg : expression.getValueArguments()) {
            // 이름이 있는 인자인지 확인 (예: onClick = ...)
            if (arg.getArgumentName() != null) {
                String argName = arg.getArgumentName().getAsName().asString();

                // onClick 인자를 찾음
                if ("onClick".equals(argName)) {
                    KtExpression argExpr = arg.getArgumentExpression();
                    if (argExpr != null) {
                        // 람다 표현식 전체를 문자열로 반환
                        return argExpr.getText();
                    }
                }
            }
        }

        return null;
    }

    /**
     * modifier 파라미터 코드 추출
     * 예: modifier = Modifier.fillMaxWidth().testTag("btn_send")
     */
    private String extractModifierParameter(KtCallExpression expression) {
        for (ValueArgument arg : expression.getValueArguments()) {
            if (arg.getArgumentName() != null) {
                String argName = arg.getArgumentName().getAsName().asString();

                // modifier 인자를 찾음
                if ("modifier".equals(argName)) {
                    KtExpression argExpr = arg.getArgumentExpression();
                    if (argExpr != null) {
                        // Modifier 체인 전체를 문자열로 반환
                        return argExpr.getText();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Modifier에서 testTag 값 추출
     * 예: Modifier.testTag("btn_send") -> "btn_send"
     */
    private String extractComposableId(String modifierCode) {
        if (modifierCode == null) {
            return null;
        }

        // testTag("...") 패턴을 찾아서 문자열 추출
        int testTagIdx = modifierCode.indexOf("testTag(");
        if (testTagIdx != -1) {
            int startQuote = modifierCode.indexOf("\"", testTagIdx);
            if (startQuote != -1) {
                int endQuote = modifierCode.indexOf("\"", startQuote + 1);
                if (endQuote != -1) {
                    return modifierCode.substring(startQuote + 1, endQuote);
                }
            }
        }

        return null;
    }

    /**
     * Modifier의 semantics에서 contentDescription 추출
     * 예: Modifier.semantics { contentDescription = "송금" } -> "송금"
     */
    private String extractSemanticHint(String modifierCode) {
        if (modifierCode == null) {
            return null;
        }

        // semantics { contentDescription = "..." } 패턴 찾기
        int semanticsIdx = modifierCode.indexOf("semantics");
        if (semanticsIdx != -1) {
            int contentDescIdx = modifierCode.indexOf("contentDescription", semanticsIdx);
            if (contentDescIdx != -1) {
                int startQuote = modifierCode.indexOf("\"", contentDescIdx);
                if (startQuote != -1) {
                    int endQuote = modifierCode.indexOf("\"", startQuote + 1);
                    if (endQuote != -1) {
                        return modifierCode.substring(startQuote + 1, endQuote);
                    }
                }
            }
        }

        return null;
    }

    /**
     * PSI 요소의 소스 코드 라인 번호 추출
     */
    private int getLineNumber(PsiElement element) {
        try {
            // PSI 요소의 텍스트 범위에서 시작 오프셋을 가져옴
            int startOffset = element.getTextRange().getStartOffset();

            // 텍스트를 줄바꿈 기준으로 나눠서 라인 번호 계산
            String text = element.getContainingFile().getText();
            int lineNumber = 1;
            for (int i = 0; i < startOffset && i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lineNumber++;
                }
            }
            return lineNumber;
        } catch (Exception e) {
            // 라인 번호 추출 실패 시 0 반환
            return 0;
        }
    }
}
