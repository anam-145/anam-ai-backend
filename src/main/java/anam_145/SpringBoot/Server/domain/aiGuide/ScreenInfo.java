package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniApp의 화면(Screen) 정보를 저장하는 엔티티
 *
 * Kotlin Jetpack Compose의 @Composable 함수 중 화면을 나타내는 함수의 정보를 저장한다.
 * 예: TransferScreen, HomeScreen, SettingsScreen 등
 * 각 화면은 여러 UI 요소(ComposableInfo)를 포함할 수 있다.
 */
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

    /**
     * 화면 정보 고유 식별자
     * 자동 증가(AUTO_INCREMENT) 방식으로 생성된다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 화면이 속한 MiniApp의 ID
     * MiniAppCodeIndex와의 관계를 나타낸다.
     */
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    /**
     * 화면의 이름
     * Kotlin Composable 함수명을 저장한다.
     * 예: "TransferScreen", "HomeScreen", "ProfileScreen"
     */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /**
     * 화면이 정의된 소스 파일 경로
     * 예: "app/src/main/kotlin/com/example/TransferScreen.kt"
     * 디버깅 및 추적 목적으로 사용된다.
     */
    @Column(name = "source_file", length = 500)
    private String sourceFile;

    /**
     * 이 화면이 속한 MiniApp 코드 인덱스
     *
     * LAZY 로딩: 실제로 사용될 때까지 로딩을 지연시킨다.
     * insertable/updatable = false: 이 필드는 조회 전용이며, app_id 컬럼으로 관계를 관리한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", referencedColumnName = "app_id", insertable = false, updatable = false)
    private MiniAppCodeIndex miniAppCodeIndex;

    /**
     * 이 화면에 포함된 모든 UI 요소(Composable) 목록
     *
     * Button, TextField, Text 등의 Composable 정보가 저장된다.
     * cascade: 화면이 삭제되면 포함된 모든 UI 요소도 함께 삭제된다.
     */
    @OneToMany(mappedBy = "screenInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComposableInfo> composables = new ArrayList<>();

    /**
     * MiniApp 코드 인덱스와의 양방향 관계를 설정한다.
     *
     * @param miniAppCodeIndex 부모 인덱스 엔티티
     */
    public void setMiniAppCodeIndex(MiniAppCodeIndex miniAppCodeIndex) {
        this.miniAppCodeIndex = miniAppCodeIndex;
    }

    /**
     * UI 요소를 추가하고 양방향 관계를 설정한다.
     *
     * @param composableInfo 추가할 UI 요소 정보
     */
    public void addComposable(ComposableInfo composableInfo) {
        composables.add(composableInfo);
        composableInfo.setScreenInfo(this);
    }
}
