package anam_145.SpringBoot.Server.web.controller.specification;

import anam_145.SpringBoot.Server.apiPayload.ApiResponse;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.KotlinFileContentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "MiniApp 코드 분석 API", description = "MiniApp 승인 시 업로드된 ZIP 파일 분석 관련 API")
public interface AnalyzeSpecification {

    @Operation(summary = "ZIP 파일에서 Kotlin 소스 추출", description = "업로드된 MiniApp ZIP 파일에서 모든 Kotlin 소스 파일(.kt)을 추출합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추출 성공",
                    content = @Content(schema = @Schema(implementation = KotlinFileContentDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 ZIP 파일 (비어있음, 형식 오류, Kotlin 파일 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "ZIP 추출 중 서버 오류")
    })
    ApiResponse<List<KotlinFileContentDTO>> extractKotlinFiles(MultipartFile zipFile);
}
