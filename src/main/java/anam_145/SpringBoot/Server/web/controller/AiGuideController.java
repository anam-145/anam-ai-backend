package anam_145.SpringBoot.Server.web.controller;

import anam_145.SpringBoot.Server.apiPayload.ApiResponse;
import anam_145.SpringBoot.Server.service.aiGuideService.AiGuideService;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideRequestDTO;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 가이드 API 컨트롤러
 * 사용자 질문을 받아 RAG 방식으로 맞춤형 UI 가이드를 생성한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guide")
public class AiGuideController {

    private final AiGuideService aiGuideService;

    /**
     * AI 가이드 생성 API
     * POST /api/v1/guide/query
     *
     * 사용자가 미니앱 사용 중 질문을 하면:
     * 1. DB에서 관련 UI 요소 검색
     * 2. LLM을 활용해 안내 메시지 생성
     * 3. 프론트엔드에서 오버레이 표시할 수 있는 형식으로 반환
     *
     * @param request 사용자 질문 및 앱 ID
     * @return 가이드 메시지 및 타겟 UI 요소 정보
     */
    @PostMapping("/query")
    public ApiResponse<GuideResponseDTO> generateGuide(@RequestBody GuideRequestDTO request) {
        log.info("AI 가이드 요청: appId={}, userQuestion={}",
                request.getAppId(), request.getUserQuestion());

        GuideResponseDTO response = aiGuideService.generateGuide(request);

        log.info("AI 가이드 생성 완료: appId={}, steps count={}",
                response.getAppId(),
                response.getSteps() != null ? response.getSteps().size() : 0);

        if (response.getSteps() != null && !response.getSteps().isEmpty()) {
            log.info("첫 번째 스텝: stepNumber={}, targetScreen={}, message={}",
                    response.getSteps().get(0).getStepNumber(),
                    response.getSteps().get(0).getTargetScreen(),
                    response.getSteps().get(0).getGuideMessage());
        }

        return ApiResponse.onSuccess(response);
    }
}
