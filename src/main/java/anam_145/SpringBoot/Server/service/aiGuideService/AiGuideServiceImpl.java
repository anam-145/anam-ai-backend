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

        // 2. DB에서 해당 앱의 모든 UI 요소 가져오기 (키워드 추출에 사용)
        List<ComposableInfo> allElements = composableInfoRepository.findByAppId(targetAppId);

        if (allElements.isEmpty()) {
            log.warn("해당 appId의 UI 요소가 DB에 없음: {}", targetAppId);
            return buildNoResultResponse(targetAppId);
        }

        // 3. DB 정보를 바탕으로 사용자 질문에서 키워드 추출
        String keyword = extractKeywordWithContext(request.getUserQuestion(), allElements);
        log.info("추출된 키워드: {}", keyword);

        // 4. DB에서 관련 UI 요소 다중 검색 (최대 10개)
        List<ComposableInfo> matchedElements = composableInfoRepository.searchByKeyword(
                targetAppId,
                keyword
        );

        if (matchedElements.isEmpty()) {
            log.warn("검색 결과 없음: appId={}, keyword={}", targetAppId, keyword);
            return buildNoResultResponse(targetAppId);
        }

        // 검색 결과를 최대 10개로 제한
        List<ComposableInfo> limitedElements = matchedElements.size() > 10
                ? matchedElements.subList(0, 10)
                : matchedElements;

        log.info("매칭된 UI 요소 개수: {}", limitedElements.size());

        // 3. LLM을 활용하여 단계별 시퀀스 생성
        List<GuideStepDTO> steps = generateStepSequence(request.getUserQuestion(), limitedElements);

        // steps가 비어있으면 예외 발생 (generateStepSequence에서 처리됨)

        // 4. 응답 DTO 생성 (appId 포함)
        return GuideResponseDTO.builder()
                .appId(targetAppId)
                .steps(steps)
                .build();
    }

    /**
     * 사용자 질문으로부터 적절한 appId 결정
     * 간단한 키워드 매칭 방식 사용 (향후 LLM 기반 분류로 개선 가능)
     */
    private String determineAppIdFromQuestion(String userQuestion) {
        String lowerQuestion = userQuestion.toLowerCase();

        // BonMedia 관련 키워드
        if (lowerQuestion.contains("본미디어") || lowerQuestion.contains("bonmedia")) {
            return "com.anam.6nqxb5qfm5lptbc9";  // BonMedia appId
        }

        // Busanilbo 관련 키워드
        if (lowerQuestion.contains("부산일보") || lowerQuestion.contains("busanilbo")) {
            return "com.anam.vh7lpswl75iqdarh";  // Busanilbo appId
        }

        // 이더리움 관련 키워드 (비트코인보다 먼저 체크)
        if (lowerQuestion.contains("이더리움") || lowerQuestion.contains("eth") ||
            lowerQuestion.contains("ethereum")) {
            return "com.anam.osba5s0oy5582dc0";  // Ethereum Wallet appId
        }

        // 비트코인 관련 키워드 (일반 키워드 제거)
        if (lowerQuestion.contains("비트코인") || lowerQuestion.contains("btc") ||
            lowerQuestion.contains("bitcoin")) {
            return "com.anam.rehrxj11f38gn09k";  // Bitcoin Wallet appId
        }

        // 기본값: 첫 번째 미니앱
        log.warn("질문으로부터 appId를 결정할 수 없음. 기본값 사용: {}", userQuestion);
        return "com.anam.rehrxj11f38gn09k";  // 기본값으로 Bitcoin Wallet
    }

    /**
     * DB 컨텍스트를 활용하여 사용자 질문에서 핵심 키워드 추출
     * LLM에게 실제 DB에 존재하는 UI 요소 정보를 제공하여 더 정확한 키워드 추출
     */
    private String extractKeywordWithContext(String userQuestion, List<ComposableInfo> allElements) {
        try {
            // DB에서 고유한 composableId, text, type 수집 (샘플로 최대 30개)
            StringBuilder dbContext = new StringBuilder();
            dbContext.append("이 앱에서 사용 가능한 UI 요소들:\n");

            int count = 0;
            for (ComposableInfo elem : allElements) {
                if (count >= 30) break; // 너무 많으면 LLM 컨텍스트 초과

                String id = elem.getComposableId() != null ? elem.getComposableId() : "no-id";
                String text = elem.getText() != null && !elem.getText().isEmpty() ? elem.getText() : "";
                String type = elem.getType() != null ? elem.getType() : "";

                dbContext.append(String.format("- %s (%s) %s\n", id, type, text));
                count++;
            }

            String systemPrompt = """
                    당신은 키워드 추출 전문가입니다.
                    사용자의 한글 질문을 분석하여 UI 요소 검색에 적합한 키워드를 추출합니다.

                    규칙:
                    1. 제공된 UI 요소 목록을 참고하여 실제로 존재하는 요소와 관련된 키워드만 추출하세요
                    2. 한글을 영어로 번역하세요 (예: "송금" -> "send", "받기" -> "receive")
                    3. composableId, text, type 중 관련된 단어를 우선적으로 선택하세요
                    4. 가장 관련성 높은 단일 키워드만 반환하세요 (여러 개 금지)
                    5. 추가 설명 없이 키워드만 반환하세요

                    예시:
                    - "비트코인 송금하는 방법 알려줘" + UI에 "action-btn Send" 있음 -> "send"
                    - "지갑 설정 어떻게 해?" + UI에 "settings-btn Settings" 있음 -> "settings"
                    - "받는 주소 확인하고 싶어" + UI에 "copy-btn receive" 있음 -> "receive"
                    """;

            String userPrompt = dbContext.toString() + "\n질문: \"" + userQuestion + "\"\n\n가장 관련성 높은 키워드 하나:";

            String keyword = openAiClientService.generateGuideMessage(systemPrompt, userPrompt);

            if (keyword != null && !keyword.isBlank()) {
                // 여러 단어가 반환되면 첫 번째 단어만 사용
                String cleanedKeyword = keyword.trim().split("\\s+")[0];
                log.info("LLM 키워드 추출: \"{}\" -> \"{}\"", userQuestion, cleanedKeyword);
                return cleanedKeyword;
            }
        } catch (Exception e) {
            log.warn("LLM 키워드 추출 실패, 기본 키워드 사용: {}", e.getMessage());
        }

        // LLM 실패 시 질문에서 간단한 키워드 추론
        String lowerQuestion = userQuestion.toLowerCase();
        if (lowerQuestion.contains("송금") || lowerQuestion.contains("보내")) {
            return "send";
        } else if (lowerQuestion.contains("받") || lowerQuestion.contains("주소")) {
            return "receive";
        } else if (lowerQuestion.contains("설정")) {
            return "settings";
        }

        return "send"; // 기본값
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
                사용자의 목표를 달성하기 위한 단계별 가이드를 생성합니다.

                규칙:
                1. 각 단계는 논리적 순서로 정렬되어야 합니다.
                2. 사용자가 따라가기 쉬워야 합니다.
                3. 간결하고 친절한 한국어로 작성하세요.
                4. 응답은 반드시 JSON 형식이어야 합니다.
                """;
    }

    /**
     * 사용자 프롬프트: UI 요소 목록 + 시퀀스 생성 요청
     */
    private String buildUserPromptForSequence(String userQuestion, List<ComposableInfo> elements) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자 질문: \"").append(userQuestion).append("\"\n\n");
        prompt.append("관련 UI 요소들:\n");

        for (int i = 0; i < elements.size(); i++) {
            ComposableInfo elem = elements.get(i);
            prompt.append(String.format(
                    "%d. [%s 페이지] %s (%s, text: \"%s\", onClick: %s)\n",
                    i + 1,
                    elem.getScreenInfo() != null ? elem.getScreenInfo().getName() : "Unknown",
                    elem.getComposableId() != null ? elem.getComposableId() : "no-id",
                    elem.getType(),
                    elem.getText() != null ? elem.getText() : "",
                    elem.getOnClickCode() != null ? elem.getOnClickCode() : "none"
            ));
        }

        prompt.append("""

                위 요소들을 논리적 순서로 정렬하여 단계별 가이드를 JSON 형식으로 생성하세요.

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

                주의: elementIndex는 위 목록의 인덱스(0부터 시작)입니다.
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
