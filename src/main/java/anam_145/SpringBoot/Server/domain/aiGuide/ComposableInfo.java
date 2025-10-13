package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;

/**
 * Jetpack Compose UI 요소(Composable)의 상세 정보를 저장하는 엔티티
 *
 * Kotlin 코드에서 파싱한 Button, TextField, Text 등의 UI 요소 정보를 저장한다.
 * 이 정보는 사용자의 질문에 답변할 때 관련 UI 요소를 찾는데 사용되며,
 * Full-Text Search를 통해 키워드 기반 검색이 가능하다.
 *
 * 예시 코드:
 * <pre>
 * Button(
 *     modifier = Modifier.testTag("btn_send").semantics { contentDescription = "송금" },
 *     onClick = { viewModel.transfer() }
 * ) {
 *     Text("보내기")
 * }
 * </pre>
 * 위 코드는 다음과 같이 저장된다:
 * - type: "Button"
 * - composableId: "btn_send"
 * - text: "보내기"
 * - semanticHint: "송금"
 * - onClickCode: "{ viewModel.transfer() }"
 */
@Entity
@Table(name = "composable_info", indexes = {
        @Index(name = "idx_app_id", columnList = "app_id"),
        @Index(name = "idx_type", columnList = "type"),
        @Index(name = "idx_composable_id", columnList = "composable_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ComposableInfo {

    /**
     * UI 요소 고유 식별자
     * 자동 증가 방식으로 생성된다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 UI 요소가 속한 MiniApp의 ID
     */
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    /**
     * 이 UI 요소가 속한 화면의 ID
     * ScreenInfo와의 관계를 나타내며, 조회 전용 필드이다.
     */
    @Column(name = "screen_id", insertable = false, updatable = false)
    private Long screenId;

    /**
     * UI 요소의 타입
     * Jetpack Compose의 Composable 함수명을 저장한다.
     * 예: "Button", "TextField", "Text", "Image", "Card" 등
     */
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    /**
     * UI 요소의 식별자
     * Modifier.testTag()로 지정된 값이나 다른 고유 식별자를 저장한다.
     * 예: "btn_send", "input_amount", "text_balance"
     * 클라이언트에서 이 값을 사용하여 특정 UI 요소를 찾아 하이라이트할 수 있다.
     */
    @Column(name = "composable_id", length = 200)
    private String composableId;

    /**
     * UI 요소에 표시되는 텍스트
     * Button의 라벨, TextField의 placeholder, Text의 내용 등을 저장한다.
     * 예: "보내기", "금액 입력", "현재 잔액: 1000원"
     */
    @Column(name = "text", length = 500)
    private String text;

    /**
     * UI 요소의 의미적 힌트
     * Modifier.semantics의 contentDescription 값을 저장한다.
     * 접근성 및 검색 정확도 향상에 사용된다.
     * 예: "송금", "금액 입력 필드", "잔액 표시"
     */
    @Column(name = "semantic_hint", length = 500)
    private String semanticHint;

    /**
     * 클릭 이벤트 핸들러 코드
     * onClick 람다 함수의 내용을 문자열로 저장한다.
     * 예: "{ viewModel.transfer() }", "{ navController.navigate('home') }"
     * 이 정보를 통해 어떤 동작을 수행하는 버튼인지 파악할 수 있다.
     */
    @Column(name = "onclick_code", columnDefinition = "TEXT")
    private String onClickCode;

    /**
     * Modifier 체인 코드
     * UI 요소에 적용된 Modifier의 전체 코드를 저장한다.
     * 예: "Modifier.fillMaxWidth().padding(16.dp).testTag('btn_send')"
     * 레이아웃 정보나 추가적인 속성을 파악하는데 사용된다.
     */
    @Column(name = "modifier_code", columnDefinition = "TEXT")
    private String modifierCode;

    /**
     * UI 요소가 정의된 소스 파일 경로
     * 디버깅 및 코드 추적에 사용된다.
     */
    @Column(name = "source_file", length = 500)
    private String sourceFile;

    /**
     * UI 요소가 정의된 코드의 라인 번호
     * 소스 코드에서 정확한 위치를 찾는데 사용된다.
     */
    @Column(name = "line_number")
    private Integer lineNumber;

    /**
     * 검색용 통합 텍스트
     * text, semanticHint, onClickCode, composableId, screenName을 합친 문자열이다.
     * MySQL Full-Text Search 또는 LIKE 검색에 사용된다.
     * PrePersist와 PreUpdate 시 자동으로 생성된다.
     */
    @Column(name = "searchable_text", columnDefinition = "TEXT")
    private String searchableText;

    /**
     * 이 UI 요소가 속한 화면 정보
     * LAZY 로딩으로 필요할 때만 데이터를 가져온다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", referencedColumnName = "id")
    private ScreenInfo screenInfo;

    /**
     * 부모 UI 요소 (중첩 구조인 경우)
     * 예: Column 안의 Button, Card 안의 Text 등
     * 현재는 사용하지 않지만, 향후 복잡한 UI 구조 분석에 활용 가능하다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ComposableInfo parent;

    /**
     * 화면 정보를 설정하고 screenId를 동기화한다.
     *
     * @param screenInfo 이 UI 요소가 속한 화면
     */
    public void setScreenInfo(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
        this.screenId = screenInfo.getId();
    }

    /**
     * 엔티티가 저장되거나 수정되기 전에 검색용 텍스트를 자동으로 생성한다.
     *
     * 다음 필드들을 공백으로 구분하여 하나의 문자열로 합친다:
     * - text: 화면에 표시되는 텍스트
     * - semanticHint: 의미적 힌트
     * - onClickCode: 클릭 동작 코드
     * - composableId: UI 요소 ID
     * - screenName: 화면 이름
     *
     * 생성된 문자열은 searchableText 필드에 저장되어 검색에 사용된다.
     */
    @PrePersist
    @PreUpdate
    public void buildSearchableText() {
        StringBuilder sb = new StringBuilder();
        if (text != null) sb.append(text).append(" ");
        if (semanticHint != null) sb.append(semanticHint).append(" ");
        if (onClickCode != null) sb.append(onClickCode).append(" ");
        if (composableId != null) sb.append(composableId).append(" ");
        if (screenInfo != null && screenInfo.getName() != null) {
            sb.append(screenInfo.getName());
        }
        this.searchableText = sb.toString().trim();
    }
}
