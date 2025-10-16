package anam_145.SpringBoot.Server.web.dto.AiGuideDTO;

import lombok.*;

/**
 * AI 가이드 생성 요청 DTO
 * 사용자가 미니앱 사용 중 질문을 하면 이 DTO로 요청을 받는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideRequestDTO {

    /**
     * MiniApp ID
     * 예: "com.anam.bitcoin"
     */
    private String appId;

    /**
     * 사용자 질문
     * 예: "비트코인 보내는 방법", "주소 입력하는 곳"
     */
    private String userQuestion;
}
