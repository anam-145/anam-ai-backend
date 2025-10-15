package anam_145.SpringBoot.Server.service.llm;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI API 클라이언트 서비스
 * GPT 모델을 활용하여 AI 가이드 메시지를 생성한다.
 */
@Slf4j
@Service
public class OpenAiClientService {

    private final String apiKey;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;
    private final com.theokanning.openai.service.OpenAiService openAiClient;

    public OpenAiClientService(
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model:gpt-4}") String model,
            @Value("${ai.openai.max-tokens:500}") Integer maxTokens,
            @Value("${ai.openai.temperature:0.7}") Double temperature
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;

        // API 키가 설정되어 있으면 실제 클라이언트 생성, 없으면 null (mock 모드)
        if (apiKey != null && !apiKey.isBlank()) {
            this.openAiClient = new com.theokanning.openai.service.OpenAiService(apiKey, Duration.ofSeconds(60));
            log.info("OpenAI API 클라이언트 초기화 완료: model={}", model);
        } else {
            this.openAiClient = null;
            log.warn("OpenAI API 키가 설정되지 않음 - Mock 모드로 실행");
        }
    }

    /**
     * GPT 모델을 사용하여 가이드 메시지 생성
     *
     * @param systemPrompt 시스템 프롬프트 (AI의 역할 정의)
     * @param userPrompt 사용자 프롬프트 (실제 질문 및 컨텍스트)
     * @return AI가 생성한 가이드 메시지
     */
    public String generateGuideMessage(String systemPrompt, String userPrompt) {
        // API 키가 없으면 null 반환 (호출자가 mock 응답 생성)
        if (openAiClient == null) {
            log.debug("OpenAI API 키 없음 - null 반환");
            return null;
        }

        try {
            log.debug("OpenAI API 호출 시작: model={}", model);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", userPrompt)
                    ))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            ChatCompletionResult result = openAiClient.createChatCompletion(request);

            String generatedMessage = result.getChoices().get(0).getMessage().getContent();
            log.info("OpenAI API 호출 성공: 응답 길이={}자", generatedMessage.length());

            return generatedMessage;

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
            throw new RuntimeException("LLM API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * API 키 설정 여부 확인
     */
    public boolean isApiKeyConfigured() {
        return openAiClient != null;
    }
}
