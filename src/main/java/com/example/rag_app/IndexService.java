package com.example.rag_app;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.tree.retriever.api.TreeIndexBuilder;
import com.example.tree.retriever.core.DocumentTreeNode;
import com.example.tree.retriever.indexing.BaselinePipelines;
import com.example.tree.retriever.indexing.SplitterConfig;
import com.example.tree.retriever.store.TreeIndexStore;

@Service
public class IndexService {

    private final TreeIndexBuilder builder;
    private final TreeIndexStore store;

    public IndexService(TreeIndexBuilder builder, TreeIndexStore store) {
        this.builder = builder;
        this.store = store;
    }

    public void build(String content) {

        var pipeline = BaselinePipelines.markdown();

        var chunks = pipeline.process(content, new SplitterConfig(800, 100));

        // Convert ParsedChunk to DocumentTreeNode
        List<DocumentTreeNode> nodes = chunks.stream()
                .map(chunk -> new DocumentTreeNode(
                        UUID.randomUUID().toString(),
                        null,
                        chunk.text(),
                        chunk.metadata(),
                        List.of()
                ))
                .collect(Collectors.toList());

        var tree = builder.build(content, nodes);

        store.publish(tree);
    }

    public boolean hasIndex() {
        return store.current().isPresent();
    }
}
