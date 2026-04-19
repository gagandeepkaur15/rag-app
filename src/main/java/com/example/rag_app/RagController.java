package com.example.rag_app;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;
    private final IndexService indexService;

    public RagController(RagService ragService, IndexService indexService) {
        this.ragService = ragService;
        this.indexService = indexService;
    }

    // 🔹 Build Index
    @PostMapping("/index")
    public Map<String, Object> index(@RequestBody Map<String, String> body) {

        indexService.build(body.get("content"));

        return Map.of(
                "success", true,
                "message", "Index built"
        );
    }

    // 🔹 Ask Question
    @PostMapping("/ask")
    public List<Map<String, Object>> ask(@RequestBody Map<String, String> body) {

        List<Document> docs = ragService.ask(body.get("question"));

        return docs.stream().map(doc -> Map.of(
                "content", doc.getText(),
                "metadata", doc.getMetadata()
        )).toList();
    }

    // 🔹 Status
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "indexExists", indexService.hasIndex()
        );
    }
}
