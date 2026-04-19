# RAG Application - Complete Project Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Spring AI Tree Retriever Package](#spring-ai-tree-retriever-package)
4. [How It Works](#how-it-works)
5. [Core Components](#core-components)
6. [Configuration](#configuration)
7. [API Endpoints](#api-endpoints)
8. [Getting Started](#getting-started)
9. [Advanced Concepts](#advanced-concepts)

---

## Project Overview

### What is This Project?

This project is a **Retrieval-Augmented Generation (RAG) Application** built with Spring Boot that demonstrates advanced document retrieval and question-answering capabilities. It leverages the **spring-ai-tree-retriever** package to implement a sophisticated tree-based document indexing and retrieval system.

### Key Objectives

- **Index Documents**: Parse markdown documents and build an intelligent tree-based index structure
- **Smart Retrieval**: Use a hierarchical tree traversal algorithm to find the most relevant document chunks
- **AI-Powered Answers**: Leverage large language models (LLMs) to provide context-aware answers based on retrieved documents
- **Efficient Processing**: Optimize retrieval through configurable traversal strategies and result limits

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.3.5 |
| Java Version | Java | 21 |
| LLM Integration | Spring AI | 1.0.0 |
| LLM Provider | Groq (OpenAI-compatible) | llama3-70b-8192 |
| Custom Library | spring-ai-tree-retriever | 0.0.1-SNAPSHOT |
| Build Tool | Maven | Latest |

---

## Architecture

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Endpoints                      │
│  /api/index  |  /api/ask  |  /api/status                   │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
    ┌───▼──────────┐        ┌───▼──────────┐
    │ RagController│        │ RagController│
    └───┬──────────┘        └───┬──────────┘
        │                       │
   ┌────▼───────────┐    ┌──────▼───────────┐
   │ IndexService   │    │ RagService       │
   │ (Indexing)     │    │ (Retrieval)      │
   └────┬───────────┘    └──────┬───────────┘
        │                       │
   ┌────▼────────────────────────▼─────────┐
   │     spring-ai-tree-retriever          │
   │  ┌──────────────────────────────────┐ │
   │  │ TreeIndexBuilder                 │ │
   │  │ - Builds hierarchical tree index │ │
   │  │ - Organizes document chunks      │ │
   │  └──────────────────────────────────┘ │
   │  ┌──────────────────────────────────┐ │
   │  │ TreeIndexStore                   │ │
   │  │ - Stores the built tree          │ │
   │  │ - Manages index state            │ │
   │  └──────────────────────────────────┘ │
   │  ┌──────────────────────────────────┐ │
   │  │ TraversalEngine                  │ │
   │  │ - Navigates tree structure       │ │
   │  │ - Implements search algorithm    │ │
   │  │ - Returns relevant chunks        │ │
   │  └──────────────────────────────────┘ │
   │  ┌──────────────────────────────────┐ │
   │  │ TreeDocumentRetriever            │ │
   │  │ - Spring AI DocumentRetriever    │ │
   │  │ - Adapts tree traversal to RAG   │ │
   │  └──────────────────────────────────┘ │
   └────────────────────────────────────────┘
        │
   ┌────▼──────────────────────────┐
   │  LLM (Groq - LLama 3 70B)      │
   │  - Generates summaries         │
   │  - Scores relevance            │
   │  - Provides final answers       │
   └────────────────────────────────┘
```

### Component Interaction Flow

```
User Request (Question/Document)
        ↓
    RagController
        ↓
    ├─→ IndexService (if indexing)
    │   ├─→ Parse markdown content
    │   ├─→ Split into chunks
    │   ├─→ Build DocumentTreeNodes
    │   ├─→ TreeIndexBuilder.build()
    │   └─→ TreeIndexStore.publish()
    │
    └─→ RagService (if querying)
        ├─→ Create Query object
        ├─→ DocumentRetriever.retrieve()
        │   └─→ TreeDocumentRetriever
        │       └─→ TraversalEngine.traverse()
        │           ├─→ Score branches using LLM
        │           ├─→ Follow promising paths
        │           ├─→ Collect relevant chunks
        │           └─→ Return top N results
        └─→ Return Document list
```

---

## Spring AI Tree Retriever Package

### Overview

The **spring-ai-tree-retriever** package is a custom, specialized retrieval library that implements an advanced tree-based document retrieval algorithm. Unlike flat vector-based retrieval systems, this package organizes documents hierarchically and uses intelligent traversal to find relevant content.

### Why Tree-Based Retrieval?

#### Traditional Vector Retrieval
```
Query "What is RAG?"
    ↓
Convert to embedding vector
    ↓
Compare against all document vectors
    ↓
Return top N similar documents
    ↓
Problem: All documents treated equally, no hierarchical context
```

#### Tree-Based Retrieval (This Package)
```
Query "What is RAG?"
    ↓
Navigate tree intelligently:
  - Score branches using LLM relevance
  - Follow high-scoring branches deeper
  - Skip irrelevant branches early
    ↓
Collect relevant chunks at optimal depth
    ↓
Return most contextually relevant results
    ↓
Benefit: Respects document structure, efficient, context-aware
```

### Core Concepts

#### 1. **Document Tree Structure**
The tree-retriever organizes documents as a hierarchical tree where:
- **Nodes** represent document chunks or sections
- **Parent-Child relationships** represent hierarchical content organization
- **Metadata** stores information like section titles, hierarchy level, original content

```
Root (Full Document)
├── Section 1
│   ├── Subsection 1.1
│   │   ├── Chunk 1.1.1
│   │   └── Chunk 1.1.2
│   └── Subsection 1.2
└── Section 2
    ├── Chunk 2.1
    └── Chunk 2.2
```

#### 2. **Traversal Algorithm**
The `TraversalEngine` intelligently explores the tree:

**Algorithm Steps:**
1. Start at document root
2. For each node, use LLM to score children based on query relevance
3. Calculate a "branch factor" (how many branches to explore)
4. Recursively traverse the most promising branches
5. Collect chunks that meet relevance threshold
6. Return up to `maxResults` documents

**Key Parameters:**
- **branch-factor**: How many child branches to explore per node (default: 4)
- **max-results**: Maximum documents to return (default: 3)
- **traversal-deadline**: Maximum time for traversal operation (default: 3s)
- **llm-timeout**: Timeout for each LLM call (default: 2s)
- **llm-retries**: Number of retry attempts for LLM calls (default: 1)

#### 3. **Intelligent Scoring**
The LLM continuously scores nodes during traversal:
```
Query: "How does tree RAG work?"

Node: "Tree RAG Architecture"
  ├─ Child 1: "Index Building" → Score: 0.92 ✓ (High relevance)
  ├─ Child 2: "Traversal Algorithm" → Score: 0.88 ✓ (High relevance)
  ├─ Child 3: "API Reference" → Score: 0.45 ✗ (Skip)
  └─ Child 4: "Performance" → Score: 0.72 ✓ (Explore)

Result: Follow children 1, 2, and 4 (based on branch-factor)
```

### Package Components

#### **TreeIndexBuilder**
```java
Purpose: Constructs the tree index from raw documents
Input: Raw document content + DocumentTreeNode list
Output: Structured tree ready for storage and traversal

Key Method: build(content, nodes)
- Analyzes document structure
- Establishes parent-child relationships
- Prepares nodes for efficient traversal
```

#### **TreeIndexStore**
```java
Purpose: Persists and retrieves the built index
Methods:
  - publish(tree): Save the built tree
  - current(): Get the current stored tree
  - reset(): Clear the stored tree

Important: This example uses in-memory storage
Production systems would persist to database/file system
```

#### **TraversalEngine**
```java
Purpose: Implements the core tree traversal algorithm
Key Method: traverse(tree, query) -> List<Document>

Algorithm:
1. Initialize traversal with query
2. Score root's children using LLM
3. Sort by score and select top branch-factor children
4. Recursively traverse selected children
5. Enforce timeout and retry limits
6. Collect leaf nodes or relevant branches
7. Return results sorted by relevance
```

#### **TreeDocumentRetriever**
```java
Purpose: Adapts tree retrieval to Spring AI's DocumentRetriever interface
Implements: org.springframework.ai.rag.retrieval.search.DocumentRetriever

Key Method: retrieve(Query) -> List<Document>
- Wraps TraversalEngine output
- Converts tree nodes to Spring AI Documents
- Implements max-results limiting
```

#### **BaselinePipelines**
```java
Purpose: Pre-processing pipeline for content parsing
Key Methods:
  - markdown(): Process markdown documents
  - text(): Process plain text
  
What It Does:
1. Parse document structure (headers, sections)
2. Extract hierarchical information
3. Split into chunks with overlaps
4. Generate metadata for each chunk
```

#### **SplitterConfig**
```java
Configuration for document chunking:
  - chunkSize: Target size for each chunk (e.g., 800 chars)
  - overlapSize: Characters to overlap between chunks (e.g., 100)
  
Effect on chunking:
  "This is a long document..." 
    ↓
  Chunk 1: [0-800 chars]
  Chunk 2: [700-1500 chars]  ← 100 char overlap
  Chunk 3: [1400-2200 chars] ← 100 char overlap
```

### Integration with Spring Boot

The package provides **Spring Boot Auto-Configuration** that automatically:
1. Detects classpath components from the tree-retriever library
2. Creates bean instances for core components
3. Registers configuration properties (prefixed with `spring.ai.tree-retriever.`)
4. Wires dependencies automatically

**Auto-Configuration provides:**
- `TreeIndexStore` bean
- `TraversalEngine` bean
- `TreeRetrieverProperties` bean
- Configuration property scanning

---

## How It Works

### Complete Workflow Example

#### **Step 1: Indexing a Document**

**API Call:**
```bash
curl -X POST http://localhost:8081/api/index \
  -H "Content-Type: application/json" \
  -d '{"content":"# Tree RAG\nTree RAG means building an index over a document tree...'
```

**Processing Flow:**

1. **Content Received**: Raw markdown text arrives at `IndexService.build()`

2. **Parsing**: `BaselinePipelines.markdown()` parses markdown structure
   ```
   Input: "# Tree RAG\nTree RAG means... \n## How it works\n..."
   Output: ParsedChunk objects with hierarchy info
   ```

3. **Chunking**: `SplitterConfig` splits large content into overlapping chunks
   ```
   Chunk 1: "Tree RAG - building an index..."  [metadata: heading="# Tree RAG"]
   Chunk 2: "...a document tree retrieves by traversal..." [overlap from Chunk 1]
   Chunk 3: "## How it works - The tree..."   [metadata: heading="## How it works"]
   ```

4. **Tree Node Creation**: Each chunk becomes a `DocumentTreeNode`
   ```java
   new DocumentTreeNode(
       UUID.randomUUID().toString(),           // Unique ID
       null,                                   // Parent (null for now)
       "Tree RAG means building an index...",  // Content
       {"heading": "# Tree RAG", ...},         // Metadata
       List.of()                               // Children (empty initially)
   )
   ```

5. **Tree Building**: `TreeIndexBuilder.build()` establishes relationships
   ```
   TreeIndexBuilder analyzes:
   - Document hierarchy from metadata
   - Section relationships
   - Semantic connections
   
   Builds tree structure:
   Root
   ├── "# Tree RAG" (Node 1)
   │   ├── "... means building..." (Chunk 1.1)
   │   └── "... by traversal..." (Chunk 1.2)
   └── "## How it works" (Node 2)
       └── "The tree..." (Chunk 2.1)
   ```

6. **Storage**: `TreeIndexStore.publish()` saves the tree in memory
   ```
   Status: ✓ Index ready for queries
   ```

#### **Step 2: Querying the Index**

**API Call:**
```bash
curl -X POST http://localhost:8081/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"How does the tree retriever work?"}'
```

**Processing Flow:**

1. **Query Received**: Question arrives at `RagService.ask()`

2. **Query Creation**: Wrapped in `Query` object
   ```java
   new Query("How does the tree retriever work?")
   ```

3. **Retriever Invoked**: `DocumentRetriever.retrieve(query)` called
   - This delegates to `TreeDocumentRetriever`

4. **Traversal Engine Activation**: `TraversalEngine.traverse(tree, query)` begins
   ```
   Step 1: Start at root node
   
   Step 2: Score root's children for query relevance
           "How does the tree retriever work?"
           
           Node 1: "# Tree RAG" 
               → LLM Score: 0.85 (mentions tree and retriever)
           
           Node 2: "## How it works"
               → LLM Score: 0.92 (directly addresses "how it works")
           
   Step 3: Sort by score (0.92, 0.85) and select top branch-factor (4)
           → Explore both children
   
   Step 4: Traverse selected nodes recursively
           Under "## How it works":
               - Child: "The tree traversal algorithm..."
                   → LLM Score: 0.95 ✓ (Highly relevant!)
               - Child: "Configuration options..."
                   → LLM Score: 0.60 ✗ (Skip)
           
   Step 5: Continue until max-results (3) or tree exhausted
   
   Step 6: Collect results with highest scores:
           1. "The tree traversal algorithm..." [Score: 0.95]
           2. "## How it works" [Score: 0.92]
           3. "# Tree RAG... tree retriever work" [Score: 0.85]
   ```

5. **Result Collection**: Top relevant chunks returned as Documents
   ```java
   List<Document> results = [
       Document("The tree traversal algorithm..."),
       Document("## How it works"),
       Document("# Tree RAG... tree retriever work")
   ]
   ```

6. **Response Formatting**: Results converted to JSON
   ```json
   [
       {
           "content": "The tree traversal algorithm...",
           "metadata": {"heading": "How it works"}
       },
       {
           "content": "## How it works",
           "metadata": {"level": 2}
       },
       {
           "content": "# Tree RAG... tree retriever work",
           "metadata": {"heading": "Tree RAG"}
       }
   ]
   ```

---

## Core Components

### 1. RagController

**File:** [src/main/java/com/example/rag_app/RagController.java](src/main/java/com/example/rag_app/RagController.java)

**Responsibility:** HTTP endpoint management and request routing

**Endpoints:**
```java
@PostMapping("/index")
// Accepts raw content, triggers indexing
Request: {"content": "markdown or text content"}
Response: {"success": true, "message": "Index built"}

@PostMapping("/ask")
// Accepts question, retrieves relevant documents
Request: {"question": "user's question"}
Response: [{"content": "...", "metadata": {...}}, ...]

@GetMapping("/status")
// Check if index exists
Response: {"indexExists": true/false}
```

### 2. RagService

**File:** [src/main/java/com/example/rag_app/RagService.java](src/main/java/com/example/rag_app/RagService.java)

**Responsibility:** Core RAG business logic

**Key Functionality:**
```java
public List<Document> ask(String question) {
    return retriever.retrieve(new Query(question));
}
```

**How It Works:**
1. Receives a question string
2. Wraps it in a Spring AI `Query` object
3. Delegates to injected `DocumentRetriever` (TreeDocumentRetriever)
4. Returns list of relevant `Document` objects

**Dependency Injection:**
```java
private final DocumentRetriever retriever;

public RagService(DocumentRetriever retriever) {
    this.retriever = retriever;  // Auto-wired from RetrieverConfig
}
```

### 3. IndexService

**File:** [src/main/java/com/example/rag_app/IndexService.java](src/main/java/com/example/rag_app/IndexService.java)

**Responsibility:** Document indexing and tree building

**Key Methods:**

```java
public void build(String content) {
    // Step 1: Get markdown processing pipeline
    var pipeline = BaselinePipelines.markdown();
    
    // Step 2: Parse and chunk content
    var chunks = pipeline.process(content, 
        new SplitterConfig(800, 100));
    // SplitterConfig(chunkSize=800, overlapSize=100)
    
    // Step 3: Convert to tree nodes
    List<DocumentTreeNode> nodes = chunks.stream()
        .map(chunk -> new DocumentTreeNode(
            UUID.randomUUID().toString(),
            null,
            chunk.text(),
            chunk.metadata(),
            List.of()
        ))
        .collect(Collectors.toList());
    
    // Step 4: Build hierarchical tree
    var tree = builder.build(content, nodes);
    
    // Step 5: Persist the tree
    store.publish(tree);
}

public boolean hasIndex() {
    return store.current().isPresent();
}
```

**Workflow:**
1. **Parse**: Process raw content using appropriate pipeline
2. **Chunk**: Split into overlapping chunks
3. **Nodify**: Convert chunks to tree nodes
4. **Build**: Create hierarchical relationships
5. **Store**: Persist for later retrieval

### 4. ChatModelConfig

**File:** [src/main/java/com/example/rag_app/ChatModelConfig.java](src/main/java/com/example/rag_app/ChatModelConfig.java)

**Responsibility:** Configure LLM integration with Groq/OpenAI

**Configuration:**
```java
@Bean
public ChatModel chatModel() {
    OpenAiApi openAiApi = OpenAiApi.builder()
        .apiKey(apiKey)              // From application.properties
        .baseUrl(baseUrl)            // Groq's OpenAI-compatible endpoint
        .build();
    
    OpenAiChatOptions options = OpenAiChatOptions.builder()
        .model(model)                // llama3-70b-8192
        .build();
    
    return OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(options)
        .build();
}
```

**Purpose:**
- Creates Spring AI `ChatModel` bean
- Used by tree traversal engine for scoring
- Enables LLM-based relevance evaluation

### 5. RetrieverConfig

**File:** [src/main/java/com/example/rag_app/RetrieverConfig.java](src/main/java/com/example/rag_app/RetrieverConfig.java)

**Responsibility:** Wire tree retriever components into Spring context

**Key Bean:**
```java
@Bean
public DocumentRetriever documentRetriever(
    TreeIndexStore store,
    TraversalEngine engine,
    TreeRetrieverProperties properties) {
    
    return new TreeDocumentRetriever(
        store,                      // Tree storage
        engine,                     // Traversal logic
        properties.getMaxResults()  // Result limit
    );
}
```

**Integration Points:**
- `TreeIndexStore`: From tree-retriever auto-configuration
- `TraversalEngine`: From tree-retriever auto-configuration
- `TreeRetrieverProperties`: From tree-retriever auto-configuration
- Auto-configuration provides these beans automatically

### 6. AppConfiguration

**File:** [src/main/java/com/example/rag_app/AppConfiguration.java](src/main/java/com/example/rag_app/AppConfiguration.java)

**Responsibility:** Application-level configuration beans

**Currently Provides:**
```java
@Bean
public SummarizationEngine summarizationEngine() {
    return text -> {
        // Simple implementation for chunking
        String summary = text.length() > 200 
            ? text.substring(0, 200) + "..." 
            : text;
        return new SummarizationResult(summary, List.of(), false);
    };
}
```

**Purpose:**
- Provides `SummarizationEngine` bean
- Used during tree building to create node summaries
- Extensible for more sophisticated summarization

---

## Configuration

### Application Properties

**File:** [src/main/resources/application.properties](src/main/resources/application.properties)

```properties
# Application
spring.application.name=rag-app
server.port=8081

# OpenAI/Groq Configuration
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.model=llama3-70b-8192

# Tree Retriever Configuration
spring.ai.tree-retriever.max-results=3
spring.ai.tree-retriever.traversal-deadline=3s
spring.ai.tree-retriever.llm-timeout=2s
spring.ai.tree-retriever.llm-retries=1
spring.ai.tree-retriever.branch-factor=4
```

### Configuration Parameters Explained

#### **LLM Configuration**

| Parameter | Value | Explanation |
|-----------|-------|-------------|
| `api-key` | `gsk_*` | Groq API key for authentication |
| `base-url` | `https://api.groq.com/openai` | Groq uses OpenAI-compatible API |
| `model` | `llama3-70b-8192` | Llama 3 model (context: 8192 tokens) |

#### **Tree Retriever Configuration**

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `max-results` | 3 | Maximum documents returned per query |
| `traversal-deadline` | 3s | Total time budget for tree traversal |
| `llm-timeout` | 2s | Maximum time per LLM scoring call |
| `llm-retries` | 1 | Number of retries if LLM call fails |
| `branch-factor` | 4 | Maximum children to explore per node |

**Configuration Impact Examples:**

**Scenario 1: Aggressive Search (More Results)**
```properties
max-results=10
branch-factor=6
traversal-deadline=5s
llm-timeout=3s
```
Result: More comprehensive but slower searches

**Scenario 2: Fast Search (Few Results)**
```properties
max-results=1
branch-factor=2
traversal-deadline=1s
llm-timeout=500ms
```
Result: Quick response, limited results

**Scenario 3: Balanced (Current)**
```properties
max-results=3
branch-factor=4
traversal-deadline=3s
llm-timeout=2s
```
Result: Good balance between speed and quality

---

## API Endpoints

### 1. Index Endpoint

**Endpoint:** `POST /api/index`

**Purpose:** Build/rebuild the document index

**Request:**
```json
{
    "content": "# Your Document Title\n\nYour document content here...\n\n## Section\nMore content..."
}
```

**Response:**
```json
{
    "success": true,
    "message": "Index built"
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/index \
  -H "Content-Type: application/json" \
  -d '{"content":"# RAG System\n\nRAG combines retrieval and generation..."}'
```

**What Happens:**
1. Content is parsed and chunked
2. Document tree is built
3. Tree is stored in memory
4. Previous index is replaced

**Notes:**
- Overwrites existing index
- Synchronous operation
- Returns immediately after tree building

---

### 2. Ask Endpoint

**Endpoint:** `POST /api/ask`

**Purpose:** Query the index with a question

**Request:**
```json
{
    "question": "How does the system work?"
}
```

**Response:**
```json
[
    {
        "content": "Retrieved document chunk content here...",
        "metadata": {
            "heading": "# System Architecture",
            "chunk": 0,
            "level": 1
        }
    },
    {
        "content": "Another relevant chunk...",
        "metadata": {
            "heading": "## How It Works",
            "chunk": 1,
            "level": 2
        }
    }
]
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is RAG?"}'
```

**Response Processing:**
```
Question: "What is RAG?"
    ↓
Tree traversal with LLM scoring
    ↓
Found 3 relevant chunks (max-results=3)
    ↓
Return as array of documents with metadata
```

**Notes:**
- Returns up to `max-results` documents
- Documents are sorted by relevance (highest first)
- Empty array if no relevant documents found
- Requires index to exist (see `/api/status`)

---

### 3. Status Endpoint

**Endpoint:** `GET /api/status`

**Purpose:** Check if index exists

**Response:**
```json
{
    "indexExists": true
}
```

or

```json
{
    "indexExists": false
}
```

**Example:**
```bash
curl http://localhost:8081/api/status
```

**Use Case:**
- Before querying, verify index exists
- Skip querying if `indexExists` is false
- Returns immediately (no processing)

---

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Groq API Key (free at https://console.groq.com)

### Setup Instructions

#### **1. Clone and Navigate**
```bash
cd /Users/gagandeepkaur/Documents/rag_app
```

#### **2. Configure API Key**
Edit `src/main/resources/application.properties`:
```properties
spring.ai.openai.api-key=YOUR_GROQ_API_KEY_HERE
```

#### **3. Build the Project**
```bash
./mvnw clean install
```

#### **4. Run the Application**
```bash
./mvnw spring-boot:run
```

Application starts on `http://localhost:8081`

#### **5. Verify It's Running**
```bash
curl http://localhost:8081/api/status
# Should return: {"indexExists": false}
```

### First-Time Usage

#### **Step 1: Create an Index**
```bash
curl -X POST http://localhost:8081/api/index \
  -H "Content-Type: application/json" \
  -d '{
    "content":"# My Document\n\nThis is a test document.\n\n## Section 1\nContent for section 1.\n\n## Section 2\nContent for section 2."
  }'
```

Expected response:
```json
{"success": true, "message": "Index built"}
```

#### **Step 2: Check Index Status**
```bash
curl http://localhost:8081/api/status
```

Expected response:
```json
{"indexExists": true}
```

#### **Step 3: Ask a Question**
```bash
curl -X POST http://localhost:8081/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is in section 1?"}'
```

Expected response:
```json
[
  {
    "content": "Content for section 1.",
    "metadata": {"heading": "## Section 1", ...}
  },
  ...
]
```

---

## Advanced Concepts

### Document Tree Building Details

#### **How Hierarchy is Established**

The tree-retriever analyzes document structure to build relationships:

1. **Markdown Headers**: Used as primary organizational signal
   ```markdown
   # Level 1 (Root section)
   ## Level 2 (Subsection)
   ### Level 3 (Sub-subsection)
   ```

2. **Parent-Child Relationships**: Based on header hierarchy
   ```
   # Introduction
   ├── Content paragraph 1
   ├── Content paragraph 2
   └── ## Subsection
       └── Subsection content
   ```

3. **Metadata Preservation**: Each node retains context
   ```json
   {
       "heading": "## How Tree Works",
       "level": 2,
       "chunk": 0,
       "position": "middle"
   }
   ```

#### **Chunking Strategy**

The `SplitterConfig(800, 100)` means:

```
Original text:
"This is sentence 1. This is sentence 2. ... This is sentence N."
(4000+ characters total)
    ↓
Chunk 1: Characters [0-800]    "This is sentence 1. ... sentence X."
Chunk 2: Characters [700-1500] "... sentence X-1. ... sentence Y."
         └─ 100 character overlap
Chunk 3: Characters [1400-2200] "... sentence Y-1. ... sentence Z."
         └─ 100 character overlap
...
```

**Overlap Purpose:**
- Maintains context continuity
- Prevents important information from being split at boundaries
- Helps LLM understand context better

### Traversal Algorithm Deep Dive

#### **Scoring Function**

During traversal, the LLM evaluates each node:

```
Query: "How to configure tree retriever?"

Node: "Configuration"
├─ Child 1: "API Reference"
│  └─ LLM evaluation: "Does content help answer query?"
│     └─ Score: 0.88 (Mentions configuration options)
│
├─ Child 2: "Examples"
│  └─ Score: 0.75 (Shows config examples)
│
└─ Child 3: "Performance Tips"
   └─ Score: 0.45 (Not directly about configuration)

Branch Factor = 4, so explore all 3 children
Top 2 to recurse deeper: Child 1 (0.88) and Child 2 (0.75)
```

#### **Traversal Timeline**

```
Time 0ms:    Start traversal
Time 100ms:  Score root node's 5 children → select top 4
Time 300ms:  Score 4 × children → recurse on top 2 each
Time 800ms:  Score 8 × children → collect promising leaves
Time 1500ms: Score 6 × children → approach max-results limit
Time 2400ms: Sufficient results collected → stop
Time 2400ms: Return results
Deadline:    3000ms (not hit)
```

### Error Handling & Timeouts

#### **Timeout Scenarios**

```
Scenario 1: LLM Takes Too Long
├─ LLM score request sent
├─ Wait 2s (llm-timeout)
├─ No response received
└─ Retry (llm-retries=1) → if fails, skip node

Scenario 2: Overall Traversal Takes Too Long
├─ Traversal in progress
├─ 3s elapsed (traversal-deadline)
├─ Return partial results collected so far
└─ User gets best results available
```

### Performance Optimization Tips

1. **Adjust branch-factor based on tree size:**
   - Small documents (< 10KB): `branch-factor=2-3`
   - Medium documents (10-100KB): `branch-factor=4-5`
   - Large documents (> 100KB): `branch-factor=6-8`

2. **Tune chunk size for your domain:**
   - Code examples: smaller chunks (400-600 chars)
   - Technical docs: medium chunks (800-1200 chars)
   - Long-form content: larger chunks (1200-2000 chars)

3. **Adjust max-results based on use case:**
   - Real-time chat: `max-results=1-2` (fast)
   - Research: `max-results=5-10` (comprehensive)
   - Summarization: `max-results=10+` (full context)

4. **Use appropriate traversal-deadline:**
   - Chat interfaces: `500ms-1s` (snappy response)
   - Batch processing: `5-10s` (thorough search)
   - Complex queries: `3-5s` (balanced)

### Integration with External Systems

#### **Custom SummarizationEngine**

The current implementation is basic. For production:

```java
@Bean
public SummarizationEngine summarizationEngine(ChatModel chatModel) {
    return text -> {
        var response = chatModel.call(
            "Summarize in 100 words: " + text
        );
        return new SummarizationResult(
            response.getResult().getOutput().getContent(),
            List.of(),
            false
        );
    };
}
```

#### **Custom Index Storage**

Currently in-memory. For persistence:

```java
@Bean
public TreeIndexStore treeIndexStore() {
    return new DatabaseBackedTreeIndexStore(
        mongoTemplate,  // Or any database
        redisTemplate   // For caching
    );
}
```

### Monitoring and Observability

#### **Recommended Metrics to Track**

```
1. Retrieval Performance:
   - Average response time
   - P95/P99 response times
   - Cache hit rate

2. Relevance Quality:
   - User satisfaction score
   - Click-through rate
   - Query-result similarity

3. System Health:
   - LLM API availability
   - Timeout occurrences
   - Error rates
```

#### **Logging Strategy**

Add logging to track traversal:

```java
logger.info("Query: {}", question);
logger.debug("Starting traversal, deadline: 3s");
logger.debug("Node scored: {}, score: {}", nodeId, score);
logger.info("Retrieved {} documents in {}ms", 
    results.size(), duration);
```

---

## Troubleshooting

### Common Issues

#### **Issue: "IndexExists: false" when trying to query**
```
Solution: 
1. Call POST /api/index with document content first
2. Verify response is {"success": true, ...}
3. Then call POST /api/ask
```

#### **Issue: LLM API key errors**
```
Solution:
1. Verify API key in application.properties
2. Check API key has proper permissions
3. Verify base-url is correct (https://api.groq.com/openai)
```

#### **Issue: Slow retrieval performance**
```
Solution:
1. Reduce branch-factor if index is large
2. Decrease traversal-deadline to cut off early
3. Reduce chunk-size in SplitterConfig
4. Increase llm-timeout if network is slow
```

#### **Issue: Getting irrelevant results**
```
Solution:
1. Ensure document content is well-structured (use headers)
2. Increase max-results to get more options
3. Verify LLM model supports your language
4. Test with different branch-factor values
```

---

## Summary

This RAG application demonstrates a sophisticated approach to document retrieval through:

1. **Tree-based indexing** that respects document hierarchy
2. **Intelligent traversal** using LLM-powered scoring
3. **Efficient algorithms** that find relevant content quickly
4. **Spring Boot integration** for easy deployment and configuration

The spring-ai-tree-retriever package provides the core innovation: instead of treating all documents equally, it intelligently navigates a hierarchical structure to find contextually relevant information.

---

## Additional Resources

- **Spring AI Documentation**: https://docs.spring.io/spring-ai/
- **Groq API Docs**: https://console.groq.com/docs
- **Spring Boot Reference**: https://spring.io/projects/spring-boot
- **RAG Best Practices**: https://www.langchain.com/

---

**Last Updated**: April 12, 2026
**Project Version**: 0.0.1-SNAPSHOT
**Status**: Active Development
