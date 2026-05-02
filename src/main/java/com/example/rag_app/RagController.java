package com.example.rag_app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tree.retriever.core.DocumentTreeNode;
import com.example.tree.retriever.core.TreeIndex;
import com.example.tree.retriever.store.TreeIndexStore;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;
    private final IndexService indexService;
    private final TreeIndexStore treeIndexStore;

    public RagController(RagService ragService, IndexService indexService, TreeIndexStore treeIndexStore) {
        this.ragService = ragService;
        this.indexService = indexService;
        this.treeIndexStore = treeIndexStore;
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

    // 🔹 Graph JSON for frontend visualization
    @GetMapping("/tree/graph")
    public Map<String, Object> treeGraph() {
        Optional<TreeIndex> maybeIndex = treeIndexStore.current();
        if (maybeIndex.isEmpty()) {
            return Map.of(
                    "hasIndex", false,
                    "message", "No index available. Build index first using POST /api/index",
                    "nodes", List.of(),
                    "links", List.of()
            );
        }

        TreeIndex index = maybeIndex.get();
        Map<String, DocumentTreeNode> nodesById = index.nodesById();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        Map<String, Integer> levelByNodeId = computeLevels(index.rootNodeId(), nodesById);
        Set<String> childNodeIds = collectChildNodeIds(nodesById);

        for (DocumentTreeNode node : nodesById.values()) {
            String nodeId = node.nodeId();
            int level = levelByNodeId.getOrDefault(nodeId, -1);
            boolean isRoot = nodeId.equals(index.rootNodeId());
            boolean isLeaf = node.childNodeIds() == null || node.childNodeIds().isEmpty();

            nodes.add(Map.of(
                    "id", nodeId,
                    "label", shortLabel(node.content()),
                    "description", shortDescription(node.content()),
                    "level", level,
                    "isRoot", isRoot,
                    "isLeaf", isLeaf,
                    "hasParent", childNodeIds.contains(nodeId),
                    "metadata", node.metadata() == null ? Map.of() : node.metadata()
            ));

            List<String> childIds = node.childNodeIds() == null ? List.of() : node.childNodeIds();
            for (String childId : childIds) {
                if (childId != null && nodesById.containsKey(childId)) {
                    links.add(Map.of(
                            "source", nodeId,
                            "target", childId
                    ));
                }
            }
        }

        return Map.of(
                "hasIndex", true,
                "indexId", index.indexId(),
                "rootNodeId", index.rootNodeId(),
                "nodeCount", nodes.size(),
                "edgeCount", links.size(),
                "nodes", nodes,
                "links", links
        );
    }

    // 🔹 Ready-to-open 3D view (browser)
    @GetMapping(value = "/tree/3d", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> tree3dView() {
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>Tree Index 3D View</title>
                  <style>
                    body { margin: 0; font-family: Arial, sans-serif; background: #0d1117; color: #e6edf3; }
                    #topbar { padding: 10px 14px; border-bottom: 1px solid #30363d; display: flex; gap: 16px; align-items: center; }
                    #topbar .muted { color: #8b949e; font-size: 13px; }
                    #graph { width: 100vw; height: calc(100vh - 48px); }
                  </style>
                </head>
                <body>
                  <div id="topbar">
                    <strong>Tree Index 3D Visualization</strong>
                    <span id="stats" class="muted">Loading...</span>
                  </div>
                  <div id="graph"></div>

                  <script src="https://unpkg.com/3d-force-graph"></script>
                  <script>
                    async function render() {
                      const res = await fetch('/api/tree/graph');
                      const data = await res.json();
                      const stats = document.getElementById('stats');

                      if (!data.hasIndex) {
                        stats.textContent = data.message || 'No index available';
                        return;
                      }

                      stats.textContent = `indexId=${data.indexId} | nodes=${data.nodeCount} | edges=${data.edgeCount}`;

                      const gData = {
                        nodes: data.nodes.map(n => ({
                          id: n.id,
                          name: n.label,
                          desc: n.description,
                          color: n.isRoot ? '#ff7b72' : (n.isLeaf ? '#3fb950' : '#58a6ff'),
                          val: n.isRoot ? 8 : (n.isLeaf ? 3 : 5)
                        })),
                        links: data.links
                      };

                      const Graph = ForceGraph3D()(document.getElementById('graph'))
                        .graphData(gData)
                        .nodeLabel(n => `<b>${n.name}</b><br/>${n.desc}`)
                        .nodeAutoColorBy('group')
                        .linkOpacity(0.35)
                        .backgroundColor('#0d1117');

                      Graph.d3Force('charge').strength(-180);
                    }

                    render().catch(err => {
                      document.getElementById('stats').textContent = 'Failed to load graph: ' + err.message;
                    });
                  </script>
                </body>
                </html>
                """;
        return ResponseEntity.ok(html);
    }

    private Map<String, Integer> computeLevels(String rootNodeId, Map<String, DocumentTreeNode> nodesById) {
        Map<String, Integer> levels = new HashMap<>();
        if (rootNodeId == null || !nodesById.containsKey(rootNodeId)) {
            return levels;
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(rootNodeId);
        levels.put(rootNodeId, 0);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            DocumentTreeNode node = nodesById.get(currentId);
            if (node == null) {
                continue;
            }
            int currentLevel = levels.getOrDefault(currentId, 0);

            List<String> childIds = node.childNodeIds() == null ? List.of() : node.childNodeIds();
            for (String childId : childIds) {
                if (childId != null && nodesById.containsKey(childId) && !levels.containsKey(childId)) {
                    levels.put(childId, currentLevel + 1);
                    queue.add(childId);
                }
            }
        }
        return levels;
    }

    private Set<String> collectChildNodeIds(Map<String, DocumentTreeNode> nodesById) {
        Set<String> childNodeIds = new HashSet<>();
        for (DocumentTreeNode node : nodesById.values()) {
            if (node.childNodeIds() != null) {
                childNodeIds.addAll(node.childNodeIds());
            }
        }
        return childNodeIds;
    }

    private String shortLabel(String content) {
        if (content == null || content.isBlank()) {
            return "(empty)";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 36 ? compact : compact.substring(0, 36) + "...";
    }

    private String shortDescription(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 140 ? compact : compact.substring(0, 140) + "...";
    }
}
