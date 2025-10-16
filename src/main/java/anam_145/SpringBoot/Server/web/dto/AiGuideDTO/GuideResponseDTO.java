package anam_145.SpringBoot.Server.web.dto.AiGuideDTO;

import lombok.*;

import java.util.List;

/**
 * AI 가이드 생성 응답 DTO
 * 단계별 가이드 시퀀스를 배열 형태로 반환한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideResponseDTO {

    /**
     * 단계별 가이드 시퀀스
     * 사용자 작업 완료를 위한 순차적 단계들
     */
    private List<GuideStepDTO> steps;

    // === 하위 호환성을 위한 단일 가이드 필드들 (Deprecated) ===

    /**
     * @deprecated steps 배열 사용 권장
     * 타겟 화면명
     */
    @Deprecated
    private String targetScreen;

    /**
     * @deprecated steps 배열 사용 권장
     * 타겟 UI 요소 정보
     */
    @Deprecated
    private TargetElementDTO targetElement;

    /**
     * @deprecated steps 배열 사용 권장
     * AI가 생성한 안내 메시지
     */
    @Deprecated
    private String guideMessage;

    /**
     * @deprecated steps 배열 사용 권장
     * 관련 코드 정보
     */
    @Deprecated
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
