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

        // 1. 사용자 질문에서 키워드 추출
        String keyword = extractKeyword(request.getUserQuestion());
        log.debug("추출된 키워드: {}", keyword);

        // 2. DB에서 관련 UI 요소 다중 검색 (최대 10개)
        List<ComposableInfo> matchedElements = composableInfoRepository.searchByKeyword(
                request.getAppId(),
                keyword
        );

        if (matchedElements.isEmpty()) {
            log.warn("검색 결과 없음: appId={}, keyword={}", request.getAppId(), keyword);
            return buildNoResultResponse();
        }

        // 검색 결과를 최대 10개로 제한
        List<ComposableInfo> limitedElements = matchedElements.size() > 10
                ? matchedElements.subList(0, 10)
                : matchedElements;

        log.info("매칭된 UI 요소 개수: {}", limitedElements.size());

        // 3. LLM을 활용하여 단계별 시퀀스 생성
        List<GuideStepDTO> steps = generateStepSequence(request.getUserQuestion(), limitedElements);

        if (steps.isEmpty()) {
            // LLM 시퀀스 생성 실패 시 fallback: 첫 번째 요소로 단일 가이드 생성
            log.warn("LLM 시퀀스 생성 실패, 단일 가이드로 fallback");
            return buildSingleGuideFallback(limitedElements.get(0), request.getUserQuestion());
        }

        // 4. 응답 DTO 생성
        return GuideResponseDTO.builder()
                .steps(steps)
                .build();
    }

    /**
     * 사용자 질문에서 핵심 키워드 추출
     * 현재는 단순하게 전체 질문을 키워드로 사용
     * 향후 NLP 라이브러리나 LLM을 활용해 개선 가능
     */
    private String extractKeyword(String userQuestion) {
        // TODO: 형태소 분석 또는 LLM을 활용한 키워드 추출
        // 현재는 전체 문장을 키워드로 사용
        return userQuestion.trim();
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
                log.warn("LLM 응답 없음, mock 모드 사용");
                return generateMockSequence(elements);
            }

            // 3. JSON 파싱
            List<GuideStepDTO> steps = parseStepsFromLLMResponse(llmResponse, elements);

            // 4. 단계 검증
            if (steps.isEmpty()) {
                log.warn("LLM 응답 파싱 결과 빈 배열");
                return generateMockSequence(elements);
            }

            validateStepSequence(steps);
            return steps;

        } catch (Exception e) {
            log.error("LLM 시퀀스 생성 중 예외 발생", e);
            return generateMockSequence(elements);
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
     * LLM 호출하여 안내 메시지 생성
     * OpenAI API를 사용하며, API 키가 없으면 mock 응답 반환
     */
    private String callLLM(String prompt, ComposableInfo element) {
        // 시스템 프롬프트 정의
        String systemPrompt = """
                당신은 미니앱 UI 가이드 전문가입니다.
                사용자가 미니앱을 사용할 때 UI 요소를 쉽게 찾고 사용할 수 있도록 친절하고 간결하게 안내합니다.
                1-2문장으로 핵심만 전달하며, 존댓말을 사용합니다.
                """;

        // OpenAI API 호출 시도
        String llmResponse = openAiClientService.generateGuideMessage(systemPrompt, prompt);

        // API 키가 없으면 null 반환 → mock 응답 사용
        if (llmResponse == null) {
            log.warn("OpenAI API 키 미설정 - mock 응답 사용");
            return generateMockResponse(element);
        }

        return llmResponse;
    }

    /**
     * Mock 응답 생성 (OpenAI API 키가 없을 때 사용)
     */
    private String generateMockResponse(ComposableInfo element) {
        String type = element.getType();
        String text = element.getText();
        String onClick = element.getOnClickCode();

        if (type.equals("Button") && onClick != null) {
            return String.format("이 '%s' 버튼을 누르면 %s 기능이 실행됩니다.",
                    text != null && !text.isEmpty() ? text : "버튼",
                    onClick.replace("()", "").replace("navigate", "이동")
            );
        } else if (type.startsWith("Input")) {
            return String.format("이 입력란에 %s을(를) 입력하세요.",
                    text != null ? text : "값"
            );
        } else if (type.equals("Textarea")) {
            return String.format("이 텍스트 영역에 %s을(를) 입력하세요.",
                    text != null ? text : "내용"
            );
        } else if (type.equals("Select")) {
            return "이 드롭다운 메뉴에서 원하는 옵션을 선택하세요.";
        }

        return "이 UI 요소를 사용하여 원하는 작업을 수행하세요.";
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
                .fallbackSelector(element.getModifierCode())
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
     * onClickCode에서 nextScreen 추출 (navigateTo('page') 패턴)
     */
    private String extractNextScreen(String onClickCode) {
        if (onClickCode == null || onClickCode.isBlank()) {
            return null;
        }

        // navigateTo('wallet') → wallet 추출
        Pattern pattern = Pattern.compile("navigateTo\\(['\"]([^'\"]+)['\"]\\)");
        Matcher matcher = pattern.matcher(onClickCode);

        if (matcher.find()) {
            return matcher.group(1);
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
     * Mock 시퀀스 생성 (LLM 실패 시 fallback)
     */
    private List<GuideStepDTO> generateMockSequence(List<ComposableInfo> elements) {
        List<GuideStepDTO> steps = new ArrayList<>();

        // 최대 3개 요소만 사용
        int limit = Math.min(3, elements.size());

        for (int i = 0; i < limit; i++) {
            ComposableInfo element = elements.get(i);
            String message = generateMockMessage(element);

            GuideStepDTO step = buildStepDTO(i + 1, element, message);
            steps.add(step);
        }

        return steps;
    }

    /**
     * Mock 메시지 생성
     */
    private String generateMockMessage(ComposableInfo element) {
        String type = element.getType();
        String text = element.getText();

        if (type.equals("Button")) {
            return String.format("'%s' 버튼을 누르세요.", text != null ? text : "버튼");
        } else if (type.startsWith("Input")) {
            return String.format("이 입력란에 %s을(를) 입력하세요.", text != null ? text : "값");
        }

        return "이 단계를 진행하세요.";
    }

    /**
     * 단일 가이드 fallback (하위 호환성)
     */
    private GuideResponseDTO buildSingleGuideFallback(ComposableInfo element, String userQuestion) {
        String message = generateMockMessage(element);
        GuideStepDTO singleStep = buildStepDTO(1, element, message);

        return GuideResponseDTO.builder()
                .steps(List.of(singleStep))
                .build();
    }

    /**
     * 검색 결과 없을 때 응답
     */
    private GuideResponseDTO buildNoResultResponse() {
        return GuideResponseDTO.builder()
                .steps(new ArrayList<>())
                .build();
    }
}
