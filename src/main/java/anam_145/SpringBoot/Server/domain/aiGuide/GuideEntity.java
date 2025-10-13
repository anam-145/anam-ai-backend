package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI가 생성한 사용자 가이드 정보를 저장하는 엔티티
 *
 * 사용자가 "송금하는 방법?" 같은 질문을 하면,
 * GPT-4가 분석한 결과를 바탕으로 단계별 가이드를 생성하고 이 엔티티에 저장한다.
 * 각 가이드는 여러 개의 단계(GuideStepEntity)로 구성된다.
 *
 * 예시:
 * - userQuery: "송금하는 방법?"
 * - intent: "TRANSFER"
 * - steps: [
 *     Step 1: "홈 화면에서 '송금하기' 버튼을 클릭하세요",
 *     Step 2: "받는 사람의 주소를 입력하세요",
 *     Step 3: "송금할 금액을 입력하세요",
 *     Step 4: "'보내기' 버튼을 클릭하세요"
 *   ]
 */
@Entity
@Table(name = "guide_entity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideEntity {

    /**
     * 가이드 고유 식별자
     * UUID 등의 형식으로 생성되는 고유한 ID
     */
    @Id
    @Column(name = "guide_id", length = 100, nullable = false)
    private String guideId;

    /**
     * 이 가이드가 속한 MiniApp의 ID
     */
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    /**
     * 사용자 의도 분류
     * GPT-4가 사용자 질문을 분석하여 파악한 의도를 저장한다.
     * 예: "TRANSFER" (송금), "RECEIVE" (받기), "CHECK_BALANCE" (잔액 확인) 등
     * 이후 통계 분석이나 가이드 재사용에 활용할 수 있다.
     */
    @Column(name = "intent", length = 50)
    private String intent;

    /**
     * 사용자가 입력한 원본 질문
     * 예: "송금하는 방법?", "돈 받는 방법 알려줘", "잔액은 어디서 확인해?"
     */
    @Column(name = "user_query", length = 1000, nullable = false)
    private String userQuery;

    /**
     * 가이드가 생성된 시간
     * 자동으로 현재 시간이 설정된다.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 이 가이드를 구성하는 모든 단계 목록
     *
     * OrderBy: stepNumber 순서대로 정렬되어 반환된다.
     * cascade: 가이드가 삭제되면 모든 단계도 함께 삭제된다.
     * orphanRemoval: 리스트에서 제거된 단계는 자동으로 DB에서도 삭제된다.
     */
    @OneToMany(mappedBy = "guideEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    @Builder.Default
    private List<GuideStepEntity> steps = new ArrayList<>();

    /**
     * 가이드 단계를 추가하고 양방향 관계를 설정한다.
     *
     * @param step 추가할 가이드 단계
     */
    public void addStep(GuideStepEntity step) {
        steps.add(step);
        step.setGuideEntity(this);
    }
}
