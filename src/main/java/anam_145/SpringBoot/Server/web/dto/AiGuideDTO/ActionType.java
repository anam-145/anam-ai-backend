package anam_145.SpringBoot.Server.web.dto.AiGuideDTO;

/**
 * 가이드 단계에서 사용자가 수행해야 할 액션 타입
 * 프론트엔드에서 적절한 이벤트 리스너를 부착하는데 활용된다.
 */
public enum ActionType {

    NAVIGATE, // 페이지 이동이 필요한 버튼 클릭 (예: "내 지갑" 버튼 클릭 → wallet 페이지로 이동)
    CLICK,    // 단순 클릭 액션 (페이지 이동 없음) (예: 체크박스 선택, 토글 버튼)
    INPUT,    // 사용자 입력이 필요한 필드 (예: 주소 입력, 금액 입력)
    WAIT // 자동 진행 (사용자 액션 불필요) (예: 로딩 완료 대기, 자동 화면 전환)
}
