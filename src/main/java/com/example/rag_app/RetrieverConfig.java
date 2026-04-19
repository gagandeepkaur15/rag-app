package com.example.rag_app;

import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.tree.retriever.api.TraversalEngine;
import com.example.tree.retriever.api.TreeDocumentRetriever;
import com.example.tree.retriever.spring.TreeRetrieverProperties;
import com.example.tree.retriever.store.TreeIndexStore;

/**
 * Configuration class for integrating the custom TreeRetriever library with Spring Boot.
 * 
 * This configuration explicitly provides the DocumentRetriever bean by composing
 * the TreeDocumentRetriever with dependencies from the library's auto-configuration.
 * 
 * ## Integration Steps:
 * 1. Ensure spring-ai-openai dependency is in pom.xml
 * 2. Provide OpenAI API key in application.properties: spring.ai.openai.api-key=...
 * 3. Spring Boot auto-configuration provides necessary beans
 * 4. This config class creates the DocumentRetriever bean
 * 5. RagService injects DocumentRetriever and uses it for retrieval
 */
@Configuration
public class RetrieverConfig {

    /**
     * Creates the DocumentRetriever bean for use by services that need to retrieve documents.
     * 
     * This bean is constructed from TreeDocumentRetriever, which is the custom implementation
     * from the spring-ai-tree-retriever library. It implements the Spring AI interface
     * DocumentRetriever.
     * 
     * Dependencies are automatically provided by:
     * - TreeRetrieverAutoConfiguration#treeIndexStore() - stores the document tree index
     * - TreeRetrieverAutoConfiguration#traversalEngine() - navigates the document tree  
     * - TreeRetrieverProperties - configuration properties (e.g., max results)
     * 
     * @param store the TreeIndexStore from auto-configuration
     * @param engine the TraversalEngine from auto-configuration
     * @param properties configuration properties
     * @return DocumentRetriever bean ready for injection
     */
    @Bean
    public DocumentRetriever documentRetriever(TreeIndexStore store,
                                               TraversalEngine engine,
                                               TreeRetrieverProperties properties) {
        // Create and return the tree-based document retriever
        return new TreeDocumentRetriever(store, engine, properties.getMaxResults());
    }
}



