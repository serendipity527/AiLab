package com.example.ailab.ragTest;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class NaiveRagExample {
    private static final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    interface Assistant {
        String chat(String userMessage);
    }

    public static void main(String[] args) {

        // ========== 步骤 1：加载文档 ==========
        Document document = loadDocument(
                Path.of("src/main/resources/test.txt"),
                new TextDocumentParser()
        );

        // ========== 步骤 2：配置文档分割器 ==========
        DocumentSplitter splitter = DocumentSplitters.recursive(
                500,  // 每个片段最大 500 tokens
                50   // 片段之间 50 tokens 重叠

        );

        // ========== 步骤 3：创建嵌入模型 ==========
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // ========== 步骤 4：创建嵌入存储 ==========
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // ========== 步骤 5：摄取文档 ==========
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                // 可选：文档转换器
                .documentTransformer(doc -> {
                    // 添加元数据
                    doc.metadata().put("source", "internal_docs");
                    return doc;
                })
                // 可选：文本段转换器
                .textSegmentTransformer(segment -> {
                    // 在每个片段前添加文档名
                    String fileName = segment.metadata().getString("file_name");
                    return TextSegment.from(
                            fileName + "\n" + segment.text(),
                            segment.metadata()
                    );
                })
                .build();

        ingestor.ingest(document);

        // ========== 步骤 6：创建内容检索器 ==========
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)      // 最多返回 5 个相关片段
                .minScore(0.7)      // 最低相似度阈值 0.7
                .build();

        // ========== 步骤 7：创建 ChatModel ==========
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelName("qwen-flash")
                .build();

        // ========== 步骤 8：创建 AI 助手 ==========
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // ========== 步骤 9：使用助手 ==========
        String answer = assistant.chat("请总结文档的主要内容");
        System.out.println(answer);
    }
}