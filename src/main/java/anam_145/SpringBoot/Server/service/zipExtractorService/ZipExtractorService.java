package anam_145.SpringBoot.Server.service.zipExtractorService;

import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.KotlinFileContentDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ZipExtractorService {

    /**
     * ZIP 파일에서 모든 Kotlin 소스 파일을 추출
     * @param zipFile 업로드된 ZIP 파일
     * @return 추출된 Kotlin 파일 목록
     */
    List<KotlinFileContentDTO> extractKotlinFiles(MultipartFile zipFile);
}
