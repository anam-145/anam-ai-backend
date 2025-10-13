package anam_145.SpringBoot.Server.domain.aiGuide;

import jakarta.persistence.*;
import lombok.*;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    @Column(name = "screen_id", insertable = false, updatable = false)
    private Long screenId;

    @Column(name = "type", length = 50, nullable = false)
    private String type; // Button, TextField, Text, etc.

    @Column(name = "composable_id", length = 200)
    private String composableId; // testTag or modifier identifier

    @Column(name = "text", length = 500)
    private String text; // Display text

    @Column(name = "semantic_hint", length = 500)
    private String semanticHint; // contentDescription for semantic search

    @Column(name = "onclick_code", columnDefinition = "TEXT")
    private String onClickCode; // onClick lambda code

    @Column(name = "modifier_code", columnDefinition = "TEXT")
    private String modifierCode; // Modifier chain code

    @Column(name = "source_file", length = 500)
    private String sourceFile;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "searchable_text", columnDefinition = "TEXT")
    private String searchableText; // Combined text for Full-Text Search

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", referencedColumnName = "id")
    private ScreenInfo screenInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ComposableInfo parent;

    public void setScreenInfo(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
        this.screenId = screenInfo.getId();
    }

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
