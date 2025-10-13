package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guide_step_entity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_id", length = 100, insertable = false, updatable = false)
    private String guideId;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "instruction", length = 1000, nullable = false)
    private String instruction;

    @Column(name = "target_screen", length = 200)
    private String targetScreen;

    @Column(name = "target_element", length = 200)
    private String targetElement;

    @Column(name = "highlight_bounds", columnDefinition = "TEXT")
    private String highlightBounds; // JSON format: {"x":0,"y":0,"width":100,"height":50}

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", referencedColumnName = "guide_id")
    private GuideEntity guideEntity;

    public void setGuideEntity(GuideEntity guideEntity) {
        this.guideEntity = guideEntity;
        this.guideId = guideEntity.getGuideId();
    }
}
