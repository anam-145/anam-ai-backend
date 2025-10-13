package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guide_entity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideEntity {

    @Id
    @Column(name = "guide_id", length = 100, nullable = false)
    private String guideId;

    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    @Column(name = "intent", length = 50)
    private String intent; // TRANSFER, RECEIVE, etc.

    @Column(name = "user_query", length = 1000, nullable = false)
    private String userQuery;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "guideEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    @Builder.Default
    private List<GuideStepEntity> steps = new ArrayList<>();

    public void addStep(GuideStepEntity step) {
        steps.add(step);
        step.setGuideEntity(this);
    }
}
