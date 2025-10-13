package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mini_app_code_index")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MiniAppCodeIndex {

    @Id
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    @CreationTimestamp
    @Column(name = "indexed_at", nullable = false, updatable = false)
    private LocalDateTime indexedAt;

    @OneToMany(mappedBy = "miniAppCodeIndex", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScreenInfo> screens = new ArrayList<>();

    public void addScreen(ScreenInfo screenInfo) {
        screens.add(screenInfo);
        screenInfo.setMiniAppCodeIndex(this);
    }

    public void clearScreens() {
        screens.clear();
    }
}
