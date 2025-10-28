package com.example.ailab.ragTest;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.HashMap;
import java.util.Map;

public class AdvancedRagExample {
    private static final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    interface Assistant {
        String chat(String userMessage);
    }

    //RetrievalAugmentor
    public static void RetrievalAugmentorTest() {
        // ========== 准备数据 ==========
        Document document = FileSystemDocumentLoader.loadDocument("path/to/document.txt");

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document);

        // ========== 创建 ContentRetriever ==========
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)
                .build();

        // ========== 🔥 创建 RetrievalAugmentor ==========
        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                // 其他组件使用默认实现
                .build();

        // ========== 创建 AI 助手 ==========
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4")
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)  // 使用 RetrievalAugmentor
                .build();

        // ========== 使用 ==========
        String answer = assistant.chat("文档的主要内容是什么？");
        System.out.println(answer);
    }

    //查询扩展
    public static void queryTransformerTest() {
        // ========== 准备数据 ==========
        Document document = FileSystemDocumentLoader.loadDocument("src/main/resources/test.txt");

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document);
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelName("qwen-flash")
                .build();
        // ========== 🔥 创建查询扩展转换器 ==========
        QueryTransformer queryTransformer = new ExpandingQueryTransformer(
                chatModel,
                3  // 将 1 个查询扩展为 3 个查询
        );
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)
                .build();
        // ========== 创建 RetrievalAugmentor ==========
        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)  // 添加查询转换器
                .contentRetriever(contentRetriever)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        // 用户的查询会被扩展为 3 个类似的查询，增加检索覆盖面
        String answer = assistant.chat("如何取消预订？");
        System.out.println(answer);
    }
    /**
     * 查询压缩：结合对话历史，将当前查询压缩为独立的查询
     * 适用场景：多轮对话中，需要理解上下文
     */
    public static void QueryCompressionExample(){
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4")
                .build();
        Document document = FileSystemDocumentLoader.loadDocument("src/main/resources/test.txt");
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document);
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)
                .build();

        // ========== 🔥 创建查询压缩转换器 ==========
        // 会读取聊天历史，将 "它的价格是多少？" 转换为 "iPhone 15 Pro 的价格是多少？"
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);

        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))  // 需要记忆
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        // 第一轮对话
        assistant.chat("iPhone 15 Pro 有哪些特性？");

        // 第二轮对话：查询会自动结合上下文
        String answer = assistant.chat("它的价格是多少？");  // 会被转换为 "iPhone 15 Pro 的价格"
        System.out.println(answer);
    }
    private static ContentRetriever createTechnicalDocsRetriever() {
        // 实现技术文档检索器
        return null;
    }

    private static ContentRetriever createUserManualRetriever() {
        // 实现用户手册检索器
        return null;
    }

    private static ContentRetriever createFaqRetriever() {
        // 实现FAQ检索器
        return null;
    }
    /**
     * 查询路由：根据查询内容，路由到不同的检索器
     * 适用场景：多个数据源，需要智能选择
     */
    public static void QueryRouterExample(){
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4")
                .build();

        // ========== 创建多个检索器 ==========
        ContentRetriever technicalDocsRetriever = createTechnicalDocsRetriever();
        ContentRetriever userManualRetriever = createUserManualRetriever();
        ContentRetriever faqRetriever = createFaqRetriever();

        // ========== 🔥 创建查询路由器 ==========
        Map<ContentRetriever, String> retrieverDescriptions = new HashMap<>();
        retrieverDescriptions.put(technicalDocsRetriever, "技术文档和 API 参考");
        retrieverDescriptions.put(userManualRetriever, "用户手册和操作指南");
        retrieverDescriptions.put(faqRetriever, "常见问题解答");

        // LLM 会根据查询内容，自动选择最合适的检索器
        QueryRouter queryRouter = new LanguageModelQueryRouter(
                chatModel,
                retrieverDescriptions
        );

        // ========== 创建 RetrievalAugmentor ==========
        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)  // 使用查询路由器
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        // 这个查询会被路由到 technicalDocsRetriever
        String answer1 = assistant.chat("如何使用 REST API 进行身份验证？");

        // 这个查询会被路由到 userManualRetriever
        String answer2 = assistant.chat("如何重置密码？");

        System.out.println(answer1);
        System.out.println(answer2);
    }private static ContentRetriever createContentRetriever() {
        return null;
    }
    // Re-ranking
    public static void ReRankingExample(){
        ScoringModel chatModel = S.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4")
                .build();

        ContentRetriever contentRetriever = createContentRetriever();

        // ========== 🔥 创建重排序聚合器 ==========
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(chatModel)  // 使用 LLM 重新评分
                .minScore(0.8)            // 最低分数阈值
                .build();

        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)  // 添加重排序
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        // 检索结果会被 LLM 重新评分，只保留最相关的内容
        String answer = assistant.chat("如何取消预订？");
        System.out.println(answer);
    }


    public static void main(String[] args) {
        queryTransformerTest();
    }
}