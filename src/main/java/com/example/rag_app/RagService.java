package com.example.rag_app;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final DocumentRetriever retriever;

    public RagService(DocumentRetriever retriever) {
        this.retriever = retriever;
    }

    public List<Document> ask(String question) {
        return retriever.retrieve(new Query(question));
    }
}