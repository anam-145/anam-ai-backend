package anam_145.SpringBoot.Server.web.controller;

import anam_145.SpringBoot.Server.apiPayload.ApiResponse;
import anam_145.SpringBoot.Server.service.zipExtractorService.ZipExtractorService;
import anam_145.SpringBoot.Server.web.controller.specification.AnalyzeSpecification;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.KotlinFileContentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analyze")
public class AnalyzeController implements AnalyzeSpecification {

    private final ZipExtractorService zipExtractorService; // ZIP 파일 추출 서비스 로직

    /**
     * MiniApp ZIP 파일에서 Kotlin 소스 파일 추출 API
     * POST /api/v1/analyze/kotlin-files
     *
     * @param zipFile 업로드된 ZIP 파일 (multipart/form-data)
     * @return 추출된 Kotlin 파일 목록
     */
    @Override
    @PostMapping(value = "/kotlin/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<KotlinFileContentDTO>> extractKotlinFiles(
            @RequestPart("file") MultipartFile zipFile) { // form-data의 "file" 필드로 전송

        log.info("Received ZIP file upload request: {} ({}bytes)",
                zipFile.getOriginalFilename(), zipFile.getSize());

        List<KotlinFileContentDTO> kotlinFiles = zipExtractorService.extractKotlinFiles(zipFile); // 서비스 계층 호출

        log.info("Successfully extracted {} Kotlin files", kotlinFiles.size());
        return ApiResponse.onSuccess(kotlinFiles); // 공통 응답 형식으로 래핑하여 반환
    }
}
