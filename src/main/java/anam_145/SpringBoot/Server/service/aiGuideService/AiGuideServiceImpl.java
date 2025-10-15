package anam_145.SpringBoot.Server.service.aiGuideService;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import anam_145.SpringBoot.Server.repository.ComposableInfoRepository;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideRequestDTO;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 가이드 생성 서비스 구현체
 * RAG 방식으로 DB에서 관련 UI 요소를 검색하고 LLM을 활용해 안내 메시지를 생성한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGuideServiceImpl implements AiGuideService {

    private final ComposableInfoRepository composableInfoRepository;
    // TODO: LLM 클라이언트 추가 (OpenAI, Claude 등)

    @Override
    public GuideResponseDTO generateGuide(GuideRequestDTO request) {
        log.info("AI 가이드 생성 요청: appId={}, userQuestion={}",
                request.getAppId(), request.getUserQuestion());

        // 1. 사용자 질문에서 키워드 추출
        String keyword = extractKeyword(request.getUserQuestion());
        log.debug("추출된 키워드: {}", keyword);

        // 2. DB에서 관련 UI 요소 검색
        List<ComposableInfo> matchedElements = composableInfoRepository.searchByKeyword(
                request.getAppId(),
                keyword
        );

        if (matchedElements.isEmpty()) {
            log.warn("검색 결과 없음: appId={}, keyword={}", request.getAppId(), keyword);
            return buildNoResultResponse();
        }

        // 3. 가장 관련도 높은 요소 선택 (첫 번째 결과)
        ComposableInfo targetElement = matchedElements.get(0);
        log.info("매칭된 UI 요소: type={}, composableId={}, text={}",
                targetElement.getType(), targetElement.getComposableId(), targetElement.getText());

        // 4. LLM에 전달할 프롬프트 생성
        String prompt = buildPrompt(request.getUserQuestion(), targetElement);
        log.debug("LLM 프롬프트: {}", prompt);

        // 5. LLM 호출하여 안내 메시지 생성
        String guideMessage = callLLM(prompt, targetElement);

        // 6. 응답 DTO 생성
        return buildResponse(targetElement, guideMessage);
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
     * LLM에 전달할 프롬프트 생성
     */
    private String buildPrompt(String userQuestion, ComposableInfo element) {
        return String.format("""
            사용자 질문: %s

            관련 UI 요소 정보:
            - 화면: %s
            - 요소 타입: %s
            - 표시 텍스트: %s
            - 클릭 동작: %s
            - CSS 클래스: %s
            - 의미적 힌트: %s

            위 정보를 바탕으로 사용자에게 이 UI 요소를 어떻게 사용하는지 간결하게 안내하세요.
            1-2문장으로 작성하고, 친절한 말투를 사용하세요.
            """,
                userQuestion,
                element.getScreenInfo() != null ? element.getScreenInfo().getName() : "Unknown",
                element.getType(),
                element.getText() != null ? element.getText() : "(없음)",
                element.getOnClickCode() != null ? element.getOnClickCode() : "(없음)",
                element.getModifierCode() != null ? element.getModifierCode() : "(없음)",
                element.getSemanticHint() != null ? element.getSemanticHint() : "(없음)"
        );
    }

    /**
     * LLM 호출하여 안내 메시지 생성
     * TODO: 실제 LLM API 연동 필요 (OpenAI, Claude 등)
     */
    private String callLLM(String prompt, ComposableInfo element) {
        // TODO: 실제 LLM API 호출 로직 구현
        // 현재는 mock 응답 반환
        log.warn("LLM API 미구현 - mock 응답 반환");

        // mock 응답 생성 로직
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
     * GuideResponseDTO 생성
     */
    private GuideResponseDTO buildResponse(ComposableInfo element, String guideMessage) {
        GuideResponseDTO.TargetElementDTO targetElementDTO = GuideResponseDTO.TargetElementDTO.builder()
                .composableId(element.getComposableId())
                .fallbackSelector(element.getModifierCode()) // class를 fallback selector로 사용
                .type(element.getType())
                .text(element.getText())
                .build();

        return GuideResponseDTO.builder()
                .targetScreen(element.getScreenInfo() != null
                        ? element.getScreenInfo().getName()
                        : "Unknown")
                .targetElement(targetElementDTO)
                .guideMessage(guideMessage)
                .relatedCode(element.getOnClickCode())
                .build();
    }

    /**
     * 검색 결과 없을 때 응답
     */
    private GuideResponseDTO buildNoResultResponse() {
        return GuideResponseDTO.builder()
                .targetScreen(null)
                .targetElement(null)
                .guideMessage("죄송합니다. 관련된 UI 요소를 찾을 수 없습니다. 다른 방식으로 질문해주세요.")
                .relatedCode(null)
                .build();
    }
}
