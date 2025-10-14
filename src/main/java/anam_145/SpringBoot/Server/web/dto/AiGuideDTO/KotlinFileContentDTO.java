package anam_145.SpringBoot.Server.web.dto.AiGuideDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(title = "Kotlin 파일 내용 DTO", description = "ZIP 파일에서 추출한 Kotlin 소스 파일의 내용")
public class KotlinFileContentDTO {

    @Schema(description = "파일 경로", example = "app/src/main/kotlin/com/example/TransferScreen.kt")
    private final String fileName; // ZIP 파일 내부의 상대 경로

    @Schema(description = "Kotlin 소스 코드 내용", example = "@Composable\\nfun TransferScreen() {\\n    Button(onClick = { }) {\\n        Text(\"보내기\")\\n    }\\n}")
    private final String content; // UTF-8 인코딩된 전체 파일 내용

    @Schema(description = "파일 크기 (바이트)", example = "1024")
    private final long fileSize; // 실제 파일 크기 (압축 해제 후)
}
