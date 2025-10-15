package anam_145.SpringBoot.Server.service.aiGuideService;

import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideRequestDTO;
import anam_145.SpringBoot.Server.web.dto.AiGuideDTO.GuideResponseDTO;

/**
 * AI 가이드 생성 서비스 인터페이스
 * RAG(Retrieval Augmented Generation) 방식으로 사용자 질문에 대한 맞춤형 UI 가이드를 생성한다.
 */
public interface AiGuideService {

    /**
     * 사용자 질문에 대한 AI 가이드 생성
     *
     * 처리 플로우:
     * 1. 사용자 질문에서 키워드 추출
     * 2. DB에서 관련 UI 요소 검색 (searchableText 활용)
     * 3. 검색된 UI 요소 정보를 컨텍스트로 LLM에 전달
     * 4. LLM 응답을 바탕으로 GuideResponseDTO 생성
     *
     * @param request 사용자 질문 및 앱 ID
     * @return 가이드 메시지 및 타겟 UI 요소 정보
     */
    GuideResponseDTO generateGuide(GuideRequestDTO request);
}
