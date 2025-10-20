package anam_145.SpringBoot.Server.service.aiGuideService;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import anam_145.SpringBoot.Server.repository.ComposableInfoRepository;
import anam_145.SpringBoot.Server.service.llm.OpenAiClientService;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.ActionType;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideRequestDTO;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideResponseDTO;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideStepDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 가이드 생성 서비스 구현체
 * RAG 방식으로 DB에서 관련 UI 요소를 검색하고 LLM을 활용해 안내 메시지를 생성한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGuideServiceImpl implements AiGuideService {

    private final ComposableInfoRepository composableInfoRepository;
    private final OpenAiClientService openAiClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GuideResponseDTO generateGuide(GuideRequestDTO request) {
        log.info("AI 가이드 생성 요청: appId={}, userQuestion={}",
                request.getAppId(), request.getUserQuestion());

        // 1. appId가 비어있으면 질문으로부터 결정
        String targetAppId = request.getAppId();
        if (targetAppId == null || targetAppId.isBlank()) {
            targetAppId = determineAppIdFromQuestion(request.getUserQuestion());
            log.info("질문으로부터 appId 결정: {}", targetAppId);
        }

        // 2. DB에서 해당 앱의 모든 UI 요소 가져오기
        List<ComposableInfo> allElements = composableInfoRepository.findByAppId(targetAppId);

        if (allElements.isEmpty()) {
            log.warn("해당 appId의 UI 요소가 DB에 없음: {}", targetAppId);
            return buildNoResultResponse(targetAppId);
        }

        log.info("조회된 UI 요소 개수: {}", allElements.size());

        // 3. LLM을 활용하여 전체 UI 요소에서 적합한 요소 선택 및 단계별 시퀀스 생성
        List<GuideStepDTO> steps = generateStepSequence(request.getUserQuestion(), allElements);

        // steps가 비어있으면 예외 발생 (generateStepSequence에서 처리됨)

        // 4. 응답 DTO 생성 (appId 포함)
        return GuideResponseDTO.builder()
                .appId(targetAppId)
                .steps(steps)
                .build();
    }

    /**
     * 사용자 질문으로부터 적절한 appId 결정
     * 하이브리드 방식: 명확한 키워드는 즉시 매칭, 불명확하면 LLM 호출
     */
    private String determineAppIdFromQuestion(String userQuestion) {
        String lowerQuestion = userQuestion.toLowerCase();

        // BonMedia 관련 키워드
        if (lowerQuestion.contains("본미디어") || lowerQuestion.contains("bonmedia") ||
            lowerQuestion.contains("뉴스") || lowerQuestion.contains("기사")) {
            return "com.anam.6nqxb5qfm5lptbc9";  // BonMedia appId
        }

        // Busanilbo 관련 키워드
        if (lowerQuestion.contains("부산일보") || lowerQuestion.contains("busanilbo") ||
            lowerQuestion.contains("부산")) {
            return "com.anam.vh7lpswl75iqdarh";  // Busanilbo appId
        }

        // 이더리움 관련 키워드 (비트코인보다 먼저 체크)
        if (lowerQuestion.contains("이더리움") || lowerQuestion.contains("eth") ||
            lowerQuestion.contains("ethereum") || lowerQuestion.contains("이더")) {
            return "com.anam.osba5s0oy5582dc0";  // Ethereum Wallet appId
        }

        // 비트코인 관련 키워드
        if (lowerQuestion.contains("비트코인") || lowerQuestion.contains("btc") ||
            lowerQuestion.contains("bitcoin")) {
            return "com.anam.rehrxj11f38gn09k";  // Bitcoin Wallet appId
        }

        // 이락 관련 키워드
        if (lowerQuestion.contains("이락코인") || lowerQuestion.contains("이락") ||
                lowerQuestion.contains("iraccoin")) {
            return "com.anam.rehrxj11f38gn09k";  // Bitcoin Wallet appId
        }

        // 불명확한 경우 LLM에게 물어보기
        log.info("명확한 키워드 없음. LLM으로 appId 결정: {}", userQuestion);
        return determineAppIdWithLLM(userQuestion);
    }

    /**
     * LLM을 활용하여 사용자 질문으로부터 적절한 appId 결정
     * 동의어, 불명확한 표현, 맥락 이해 지원
     */
    private String determineAppIdWithLLM(String userQuestion) {
        try {
            String systemPrompt = """
                    당신은 미니앱 선택 전문가입니다.
                    사용자 질문을 분석하여 가장 적절한 미니앱 ID를 반환하세요.

                    사용 가능한 미니앱 목록:
                    1. com.anam.rehrxj11f38gn09k
                       - 이름: Bitcoin Wallet
                       - 설명: 비트코인 블록체인 지갑

                    2. com.anam.osba5s0oy5582dc0
                       - 이름: Ethereum Wallet
                       - 설명: 이더리움 블록체인 지갑

                    3. com.anam.6nqxb5qfm5lptbc9
                       - 이름: BonMedia
                       - 설명: 뉴스/미디어 콘텐츠 서비스

                    4. com.anam.vh7lpswl75iqdarh
                       - 이름: Busanilbo
                       - 설명: 부산 지역 뉴스 서비스

                    블록체인 지갑 공통 기능:
                    - 송금 (보내기, 전송, transfer, send)
                    - 받기 (입금, 수신, receive, deposit)
                    - 잔액 조회 (balance, 잔고)
                    - 주소 확인 및 복사
                    - 거래 내역 (transaction history)
                    - 설정 (settings, 프라이빗 키, 시드 구문)

                    규칙:
                    1. 질문에서 명시된 암호화폐나 서비스 이름을 찾으세요
                    2. 동의어와 약어도 고려하세요 (예: "이더" = Ethereum, "코인" = 암호화폐)
                    3. 블록체인 공통 기능(송금, 받기 등)이면서 특정 코인이 명시되지 않은 경우 기본값 사용
                    4. 불명확하거나 일반적인 암호화폐 관련 질문이면 기본값: com.anam.rehrxj11f38gn09k
                    5. **반드시 appId만 반환하세요. 설명이나 추가 텍스트 없이 appId만 출력하세요.**

                    예시:
                    - "이더 보내기" → com.anam.osba5s0oy5582dc0
                    - "이더리움 받기" → com.anam.osba5s0oy5582dc0
                    - "암호화폐 송금" → com.anam.rehrxj11f38gn09k
                    - "코인 받기" → com.anam.rehrxj11f38gn09k
                    - "부산 뉴스" → com.anam.vh7lpswl75iqdarh
                    """;

            String userPrompt = "질문: \"" + userQuestion + "\"\n\n적절한 appId:";

            String appId = openAiClientService.generateGuideMessage(systemPrompt, userPrompt);

            if (appId == null || appId.isBlank()) {
                log.warn("LLM이 appId를 반환하지 않음. 기본값 사용");
                return "com.anam.rehrxj11f38gn09k";
            }

            // 응답 정리 (앞뒤 공백, 따옴표, 설명 제거)
            String cleanedAppId = appId.trim()
                    .replaceAll("^['\"]|['\"]$", "")  // 따옴표 제거
                    .split("\\s")[0];  // 첫 번째 단어만 (설명 제거)

            log.info("LLM appId 결정: \"{}\" -> {}", userQuestion, cleanedAppId);
            return cleanedAppId;

        } catch (Exception e) {
            log.error("LLM appId 결정 실패, 기본값 사용: {}", e.getMessage());
            return "com.anam.rehrxj11f38gn09k";  // 기본값
        }
    }


    /**
     * LLM을 활용하여 단계별 시퀀스 생성
     */
    private List<GuideStepDTO> generateStepSequence(String userQuestion, List<ComposableInfo> elements) {
        try {
            // 1. LLM 프롬프트 생성
            String systemPrompt = buildSystemPromptForSequence();
            String userPrompt = buildUserPromptForSequence(userQuestion, elements);

            log.debug("LLM 시퀀스 생성 프롬프트 길이: {} chars", userPrompt.length());

            // 2. LLM 호출
            String llmResponse = openAiClientService.generateGuideMessage(systemPrompt, userPrompt);

            if (llmResponse == null || llmResponse.isBlank()) {
                throw new RuntimeException("LLM 응답이 비어있습니다.");
            }

            // 3. JSON 파싱
            List<GuideStepDTO> steps = parseStepsFromLLMResponse(llmResponse, elements);

            // 4. 단계 검증
            if (steps.isEmpty()) {
                throw new RuntimeException("LLM 응답 파싱 결과 빈 배열입니다.");
            }

            validateStepSequence(steps);
            return steps;

        } catch (Exception e) {
            log.error("LLM 시퀀스 생성 중 예외 발생", e);
            throw new RuntimeException("AI 가이드 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 시스템 프롬프트: 단계별 가이드 생성 역할 정의
     */
    private String buildSystemPromptForSequence() {
        return """
                당신은 미니앱 UI 가이드 전문가입니다.
                사용자의 질문을 분석하여 목표를 달성하기 위한 단계별 가이드를 생성합니다.

                규칙:
                1. 제공된 모든 UI 요소 중에서 사용자 질문과 의미적으로 관련된 요소만 선택하세요.
                2. 선택된 요소들을 논리적 순서로 정렬하여 단계별 가이드를 생성하세요.
                3. 각 단계는 사용자가 따라가기 쉬워야 합니다.
                4. 간결하고 친절한 한국어로 작성하세요.
                5. 응답은 반드시 JSON 형식이어야 합니다.

                예시:
                - 질문: "비트코인 키 어디서 봐?" → "Export Private Key" 버튼 선택
                - 질문: "송금하고 싶어" → "Send" 또는 "Transfer" 버튼 선택
                - 질문: "주소 복사하고 싶어" → "Copy" 버튼 선택
                """;
    }

    /**
     * 사용자 프롬프트: UI 요소 목록 + 시퀀스 생성 요청
     */
    private String buildUserPromptForSequence(String userQuestion, List<ComposableInfo> elements) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자 질문: \"").append(userQuestion).append("\"\n\n");
        prompt.append("미니앱의 모든 UI 요소들:\n");

        for (int i = 0; i < elements.size(); i++) {
            ComposableInfo elem = elements.get(i);
            prompt.append(String.format(
                    "%d. [%s 페이지] %s (타입: %s, 텍스트: \"%s\", 검색가능텍스트: \"%s\", onClick: %s)\n",
                    i,
                    elem.getScreenInfo() != null ? elem.getScreenInfo().getName() : "Unknown",
                    elem.getComposableId() != null ? elem.getComposableId() : "no-id",
                    elem.getType(),
                    elem.getText() != null ? elem.getText() : "",
                    elem.getSearchableText() != null ? elem.getSearchableText() : "",
                    elem.getOnClickCode() != null ? elem.getOnClickCode() : "none"
            ));
        }

        prompt.append("""

                위 모든 UI 요소 중에서 사용자 질문과 관련된 요소들을 의미적으로 선택하고,
                논리적 순서로 정렬하여 단계별 가이드를 JSON 형식으로 생성하세요.

                응답 형식:
                {
                  "steps": [
                    {
                      "stepNumber": 1,
                      "elementIndex": 0,
                      "message": "사용자 친화적인 안내 메시지"
                    },
                    ...
                  ]
                }

                주의:
                1. elementIndex는 위 목록의 인덱스(0부터 시작)입니다.
                2. 사용자 질문의 의도를 파악하여 적합한 요소만 선택하세요.
                3. 텍스트나 검색가능텍스트에서 의미가 유사한 요소를 찾으세요.
                4. **중요: 목표 요소가 현재 메인 페이지가 아닌 다른 페이지에 있다면,
                   반드시 그 페이지로 이동하는 버튼을 먼저 단계에 포함시키세요.**
                   - 각 페이지의 요소들을 확인하여 논리적인 네비게이션 경로를 구성하세요.
                   - 같은 페이지 내의 요소들은 순차적으로 안내하세요.
                """);

        return prompt.toString();
    }

    /**
     * LLM JSON 응답 파싱하여 GuideStepDTO 리스트 생성
     */
    private List<GuideStepDTO> parseStepsFromLLMResponse(String llmResponse, List<ComposableInfo> elements) {
        try {
            // JSON 추출 (코드 블록 내부에 있을 수 있음)
            String jsonString = extractJsonFromResponse(llmResponse);

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode stepsNode = rootNode.get("steps");

            if (stepsNode == null || !stepsNode.isArray()) {
                log.warn("LLM 응답에 'steps' 배열이 없음");
                return new ArrayList<>();
            }

            List<GuideStepDTO> steps = new ArrayList<>();

            for (JsonNode stepNode : stepsNode) {
                int stepNumber = stepNode.get("stepNumber").asInt();
                int elementIndex = stepNode.get("elementIndex").asInt();
                String message = stepNode.get("message").asText();

                if (elementIndex < 0 || elementIndex >= elements.size()) {
                    log.warn("elementIndex 범위 초과: {}", elementIndex);
                    continue;
                }

                ComposableInfo element = elements.get(elementIndex);

                // GuideStepDTO 생성
                GuideStepDTO step = buildStepDTO(stepNumber, element, message);
                steps.add(step);
            }

            return steps;

        } catch (Exception e) {
            log.error("LLM 응답 JSON 파싱 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * LLM 응답에서 JSON 문자열 추출 (마크다운 코드 블록 제거)
     */
    private String extractJsonFromResponse(String response) {
        // ```json ... ``` 형태 제거
        String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        return cleaned.trim();
    }

    /**
     * ComposableInfo를 GuideStepDTO로 변환
     */
    private GuideStepDTO buildStepDTO(int stepNumber, ComposableInfo element, String message) {
        // TargetElementDTO 생성
        GuideResponseDTO.TargetElementDTO targetElementDTO = GuideResponseDTO.TargetElementDTO.builder()
                .composableId(element.getComposableId())
                .fallbackSelector(element.getFallbackSelector())  // 수정: modifierCode → fallbackSelector
                .type(element.getType())
                .text(element.getText())
                .build();

        // ActionType 결정
        ActionType actionType = determineActionType(element);

        // nextScreen 추출
        String nextScreen = extractNextScreen(element.getOnClickCode());

        return GuideStepDTO.builder()
                .stepNumber(stepNumber)
                .targetScreen(element.getScreenInfo() != null
                        ? element.getScreenInfo().getName()
                        : "Unknown")
                .targetElement(targetElementDTO)
                .guideMessage(message)
                .actionType(actionType)
                .nextScreen(nextScreen)
                .relatedCode(element.getOnClickCode())
                .build();
    }

    /**
     * ActionType 자동 분류
     */
    private ActionType determineActionType(ComposableInfo element) {
        String onClickCode = element.getOnClickCode();
        String type = element.getType();

        // NAVIGATE: navigateTo() 포함
        if (onClickCode != null && onClickCode.contains("navigateTo")) {
            return ActionType.NAVIGATE;
        }

        // INPUT: Input_ 타입
        if (type != null && type.startsWith("Input")) {
            return ActionType.INPUT;
        }

        // CLICK: Button 타입
        if (type != null && type.equals("Button")) {
            return ActionType.CLICK;
        }

        // 기본값: WAIT
        return ActionType.WAIT;
    }

    /**
     * onClickCode에서 nextScreen 추출
     *
     * 지원 패턴:
     * - navigateTo('page') → page
     * - navigateToSend() → send
     * - navigateToReceive() → receive
     * - navigateToSettings() → settings
     */
    private String extractNextScreen(String onClickCode) {
        if (onClickCode == null || onClickCode.isBlank()) {
            return null;
        }

        // navigateTo('wallet') → wallet 추출
        Pattern pattern1 = Pattern.compile("navigateTo\\(['\"]([^'\"]+)['\"]\\)");
        Matcher matcher1 = pattern1.matcher(onClickCode);
        if (matcher1.find()) {
            return matcher1.group(1);
        }

        // navigateToXxx() → xxx 추출 (camelCase to lowercase)
        Pattern pattern2 = Pattern.compile("navigateTo([A-Z][a-zA-Z]*)\\(");
        Matcher matcher2 = pattern2.matcher(onClickCode);
        if (matcher2.find()) {
            String screenName = matcher2.group(1);
            return screenName.toLowerCase(); // Send → send, Settings → settings
        }

        return null;
    }

    /**
     * 단계 시퀀스 검증 (순환 참조, 중복 화면 체크)
     */
    private void validateStepSequence(List<GuideStepDTO> steps) {
        if (steps.size() < 2) {
            return; // 단일 단계는 검증 불필요
        }

        // 같은 화면이 연속으로 나오는지 체크
        for (int i = 0; i < steps.size() - 1; i++) {
            String currentScreen = steps.get(i).getTargetScreen();
            String nextScreen = steps.get(i + 1).getTargetScreen();

            if (currentScreen != null && currentScreen.equals(nextScreen)) {
                log.warn("연속된 동일 화면 감지: {} (step {}-{})", currentScreen, i + 1, i + 2);
            }
        }
    }

    /**
     * 검색 결과 없을 때 응답
     */
    private GuideResponseDTO buildNoResultResponse(String appId) {
        return GuideResponseDTO.builder()
                .appId(appId)
                .steps(new ArrayList<>())
                .build();
    }
}
