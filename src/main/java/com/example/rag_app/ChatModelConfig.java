package com.example.rag_app;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.model}")
    private String model;

    @Bean
    public ChatModel chatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(model)
            .build();
        
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();
    }
}