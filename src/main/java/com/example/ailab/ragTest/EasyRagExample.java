package com.example.ailab.ragTest;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class EasyRagExample {
    private static final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    interface Assistant {
        String chat(String userMessage);
    }

    public static void main(String[] args) {

        // ========== 步骤 1：加载文档 ==========
        // 加载单个文件
        Document document = FileSystemDocumentLoader.loadDocument(
                "src/main/resources/test.txt"
        );

//        // 或加载整个目录（递归）
//        List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(
//                "path/to/your/documents"
//        );

        // ========== 步骤 2：创建嵌入存储 ==========
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // ========== 步骤 3：摄取文档（自动分割和嵌入）==========
        // 🔥 最简单方式：使用默认配置
        EmbeddingStoreIngestor.ingest(document, embeddingStore);

//        // 或者使用 Builder 方式（可自定义配置）
//        EmbeddingStoreIngestor.builder()
//                .embeddingStore(embeddingStore)
//                // documentTransformer、documentSplitter、embeddingModel
//                // 都会通过 SPI 自动加载
//                .build()
//                .ingest(documents);

        // ========== 步骤 4：创建 ChatModel ==========
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelName("qwen-flash")
                .build();

        // ========== 步骤 5：创建 AI 助手（集成 RAG）==========
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                // 🔥 关键：配置内容检索器
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();

        // ========== 步骤 6：使用助手 ==========
        String answer = assistant.chat("你的文档中有关 LSY 的内容是什么？");
        System.out.println(answer);
    }
}