package anam_145.SpringBoot.Server.web.dto.AiGuideDTO;

import lombok.*;

/**
 * 단계별 가이드 정보 DTO
 * 사용자 작업 완료를 위한 개별 단계 하나를 나타낸다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideStepDTO {

    /**
     * 단계 번호 (1부터 시작)
     */
    private Integer stepNumber;

    /**
     * 이 단계가 실행될 화면명
     * 예: "index", "wallet", "send"
     */
    private String targetScreen;

    /**
     * 타겟 UI 요소 정보
     */
    private GuideResponseDTO.TargetElementDTO targetElement;

    /**
     * 이 단계의 안내 메시지
     * 예: "'내 비트코인 지갑 페이지로 이동하기' 버튼을 누르세요"
     */
    private String guideMessage;

    /**
     * 사용자가 수행해야 할 액션 타입
     * NAVIGATE, CLICK, INPUT, WAIT
     */
    private ActionType actionType;

    /**
     * 다음 화면 (이동 액션일 경우)
     * 예: actionType이 NAVIGATE이고 nextScreen이 "wallet"이면 wallet 페이지로 이동
     */
    private String nextScreen;

    /**
     * 관련 코드 정보 (디버깅용)
     * 예: "navigateTo('wallet')", "confirmSend()"
     */
    private String relatedCode;
}
