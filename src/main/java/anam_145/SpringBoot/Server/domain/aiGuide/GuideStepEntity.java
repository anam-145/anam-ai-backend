package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;

/**
 * 가이드의 개별 단계 정보를 저장하는 엔티티
 *
 * 하나의 가이드(GuideEntity)는 여러 단계로 구성되며,
 * 각 단계는 사용자가 수행해야 할 행동과 대상 UI 요소를 포함한다.
 *
 * 예시:
 * Step 1:
 * - instruction: "홈 화면에서 '송금하기' 버튼을 클릭하세요"
 * - targetScreen: "HomeScreen"
 * - targetElement: "btn_transfer"
 *
 * Step 2:
 * - instruction: "받는 사람의 주소를 입력하세요"
 * - targetScreen: "TransferScreen"
 * - targetElement: "input_address"
 */
@Entity
@Table(name = "guide_step_entity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideStepEntity {

    /**
     * 단계 고유 식별자
     * 자동 증가 방식으로 생성된다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 단계가 속한 가이드의 ID
     * 조회 전용 필드이다.
     */
    @Column(name = "guide_id", length = 100, insertable = false, updatable = false)
    private String guideId;

    /**
     * 단계 순서 번호
     * 1부터 시작하며, 사용자에게 표시되는 순서를 나타낸다.
     * 예: 1, 2, 3, 4...
     */
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    /**
     * 사용자에게 표시할 안내 메시지
     * 명확하고 구체적인 행동 지시를 포함한다.
     * 예: "홈 화면에서 '송금하기' 버튼을 클릭하세요",
     *     "받는 사람의 주소를 입력하세요",
     *     "금액을 입력한 후 '보내기' 버튼을 클릭하세요"
     */
    @Column(name = "instruction", length = 1000, nullable = false)
    private String instruction;

    /**
     * 이 단계를 수행해야 하는 화면 이름
     * 예: "HomeScreen", "TransferScreen", "ConfirmationScreen"
     * 클라이언트는 이 값을 사용하여 사용자가 올바른 화면에 있는지 확인할 수 있다.
     */
    @Column(name = "target_screen", length = 200)
    private String targetScreen;

    /**
     * 이 단계에서 조작해야 할 UI 요소의 ID
     * ComposableInfo의 composableId와 매칭된다.
     * 예: "btn_transfer", "input_address", "btn_send"
     * 클라이언트는 이 값을 사용하여 해당 UI 요소를 찾아 하이라이트한다.
     */
    @Column(name = "target_element", length = 200)
    private String targetElement;

    /**
     * UI 요소의 하이라이트 영역 좌표 (JSON 형식)
     * 클라이언트에서 말풍선이나 하이라이트를 표시할 위치 정보를 저장한다.
     * JSON 형식 예시:
     * {
     *   "x": 50,
     *   "y": 200,
     *   "width": 300,
     *   "height": 60
     * }
     *
     * 주의: 실제 화면 크기에 따라 클라이언트에서 동적으로 계산할 수도 있으므로,
     * 이 필드는 선택적으로 사용된다.
     */
    @Column(name = "highlight_bounds", columnDefinition = "TEXT")
    private String highlightBounds;

    /**
     * 이 단계가 속한 가이드 엔티티
     * LAZY 로딩으로 필요할 때만 데이터를 가져온다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", referencedColumnName = "guide_id")
    private GuideEntity guideEntity;

    /**
     * 가이드 엔티티와의 양방향 관계를 설정하고 guideId를 동기화한다.
     *
     * @param guideEntity 이 단계가 속한 가이드
     */
    public void setGuideEntity(GuideEntity guideEntity) {
        this.guideEntity = guideEntity;
        this.guideId = guideEntity.getGuideId();
    }
}
