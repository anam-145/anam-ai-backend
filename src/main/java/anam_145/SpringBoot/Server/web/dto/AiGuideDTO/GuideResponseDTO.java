package anam_145.SpringBoot.Server.web.dto.AiGuideDTO;

import lombok.*;

/**
 * AI 가이드 생성 응답 DTO
 * 프론트엔드에서 특정 UI 요소를 찾아 오버레이 말풍선을 표시하는데 필요한 정보를 담는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideResponseDTO {

    /**
     * 타겟 화면명
     * 예: "send", "receive", "index"
     */
    private String targetScreen;

    /**
     * 타겟 UI 요소 정보
     */
    private TargetElementDTO targetElement;

    /**
     * AI가 생성한 안내 메시지
     * 예: "이 입력란에 받는 사람의 비트코인 주소를 입력하세요"
     */
    private String guideMessage;

    /**
     * 관련 코드 정보 (선택적, 디버깅용)
     * 예: "onclick: confirmSend()"
     */
    private String relatedCode;

    /**
     * 타겟 UI 요소 정보 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class TargetElementDTO {

        /**
         * UI 요소 ID (우선 selector)
         * 예: "recipient-address", "send-btn"
         */
        private String composableId;

        /**
         * fallback selector (composableId가 없거나 찾기 실패 시 사용)
         * 주로 class명
         * 예: "form-input", "send-confirm-btn"
         */
        private String fallbackSelector;

        /**
         * UI 요소 타입
         * 예: "Button", "Input_text", "Textarea"
         */
        private String type;

        /**
         * UI 요소에 표시되는 텍스트
         * 예: "Enter recipient's address", "Confirm Send"
         */
        private String text;
    }
}
