package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screen_info", indexes = {
        @Index(name = "idx_app_id", columnList = "app_id"),
        @Index(name = "idx_name", columnList = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScreenInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "source_file", length = 500)
    private String sourceFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", referencedColumnName = "app_id", insertable = false, updatable = false)
    private MiniAppCodeIndex miniAppCodeIndex;

    @OneToMany(mappedBy = "screenInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComposableInfo> composables = new ArrayList<>();

    public void setMiniAppCodeIndex(MiniAppCodeIndex miniAppCodeIndex) {
        this.miniAppCodeIndex = miniAppCodeIndex;
    }

    public void addComposable(ComposableInfo composableInfo) {
        composables.add(composableInfo);
        composableInfo.setScreenInfo(this);
    }
}
