package com.example.rag_app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.tree.retriever.indexing.summarization.SummarizationEngine;
import com.example.tree.retriever.indexing.summarization.SummarizationResult;

@Configuration
public class AppConfiguration {

    @Bean
    public SummarizationEngine summarizationEngine() {
        return text -> {
            // Simple implementation: return the first 200 characters
            String summary = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            return new SummarizationResult(summary, java.util.List.of(), false);
        };
    }
}
