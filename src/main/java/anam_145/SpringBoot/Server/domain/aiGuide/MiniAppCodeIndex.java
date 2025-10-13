package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MiniApp 코드 인덱스 정보를 저장하는 엔티티
 *
 * MiniApp이 승인되어 ZIP 파일이 업로드되면,
 * 해당 앱의 코드 분석 결과를 인덱싱한 시점의 정보를 저장한다.
 * 이 엔티티는 여러 ScreenInfo와 1:N 관계를 가진다.
 */
@Entity
@Table(name = "mini_app_code_index")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MiniAppCodeIndex {

    /**
     * MiniApp 고유 식별자
     * Primary Key로 사용되며, MiniApp 승인 시 부여된 ID
     */
    @Id
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    /**
     * 코드 인덱싱이 수행된 시간
     * MiniApp ZIP 파일이 업로드되어 AST 파싱 및 DB 저장이 완료된 시점
     * 자동으로 현재 시간이 설정되며, 이후 수정되지 않는다.
     */
    @CreationTimestamp
    @Column(name = "indexed_at", nullable = false, updatable = false)
    private LocalDateTime indexedAt;

    /**
     * 이 MiniApp에 속한 모든 화면(Screen) 정보 목록
     *
     * mappedBy: ScreenInfo의 miniAppCodeIndex 필드와 매핑
     * cascade: 부모 엔티티의 변경사항이 자식에게 전파됨
     * orphanRemoval: 부모와의 관계가 끊어진 자식은 자동으로 삭제됨
     */
    @OneToMany(mappedBy = "miniAppCodeIndex", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScreenInfo> screens = new ArrayList<>();

    /**
     * 화면 정보를 추가하고 양방향 관계를 설정한다.
     *
     * @param screenInfo 추가할 화면 정보
     */
    public void addScreen(ScreenInfo screenInfo) {
        screens.add(screenInfo);
        screenInfo.setMiniAppCodeIndex(this);
    }

    /**
     * 모든 화면 정보를 제거한다.
     * 주로 재인덱싱 시 기존 데이터를 정리할 때 사용된다.
     */
    public void clearScreens() {
        screens.clear();
    }
}
