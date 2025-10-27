package com.example.ailab.AiService;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.*;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.time.LocalDate;
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

// 第1步：定义接口
interface Assistant {
    String chat(String userMessage);
    String chat(@MemoryId int memoryId, @UserMessage String message);
    TokenStream streamChat(String message);
    @UserMessage("Generate an outline for {{it}}")
    Result<List<String>> generateOutlineFor(String topic);
}
interface MetaAssistant {
    String chat(String userMessage);
    @UserMessage("Generate an outline for {{it}}")
    Result<List<String>> generateOutlineFor(String topic);
}

interface StreamAssistant {
    TokenStream streamChat(String userMessage);

}

interface Friend {
    @SystemMessage("You are a good friend of mine. Answer using slang.")
    String chat(String userMessage);
}

interface Translator {
    @SystemMessage("You are a professional translator into {{language}}")
    @UserMessage("Translate the following text: {{text}}")
    String translate(@V("text") String text, @V("language") String language);
}

// 结构化输出 - 布尔值
interface SentimentAnalyzer {
    @UserMessage("Does {{it}} has a positive sentiment?")
    boolean isPositive(String text);

    @UserMessage("Analyze sentiment of {{it}}")
    Sentiment analyzeSentimentOf(String text);
}

enum Sentiment {
    POSITIVE, NEUTRAL, NEGATIVE
}

class Person {
    @Description("first name of a person")
    String firstName;
    String lastName;
    LocalDate birthDate;
    Address address;
}

class Address {
    String street;
    Integer streetNumber;
    String city;
}

interface PersonExtractor {
    @UserMessage("Extract information about a person from {{it}}")
    Person extractPersonFrom(String text);
}

class Calculator {
    @Tool
    int add(int a, int b) {
        return a + b;
    }

    @Tool
    int multiply(int a, int b) {
        return a * b;
    }
}






public class AiServiceTest {
    private static final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    // 基本使用
    public static void basicUse() {
        // 第2步：配置模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();

        // 第3步：创建AI Service实例
        Assistant assistant = AiServices.create(Assistant.class, model);

        // 第4步：使用
        String answer = assistant.chat("Hello");
        System.out.println(answer); // Hello, how can I help you?
    }

    //
    public static void userSystemPrompt() {
        // 第2步：配置模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();

        Friend friend = AiServices.create(Friend.class, model);
        String answer = friend.chat("Hello"); // Hey! What's up?
        System.out.println(answer);
    }

    // 使用提示词模板
    public static void userTemplate() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        Translator translator = AiServices.create(Translator.class, model);
        String result = translator.translate("Hello", "Chinese");
    }

    // 结构化输出
    public static void structOutput() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, model);
        boolean positive = analyzer.isPositive("It's wonderful!"); // true
        System.out.println(positive);
    }

    // 枚举类型输出
    public static void enumOutput() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, model);
        Sentiment sentiment = analyzer.analyzeSentimentOf("I love you"); // POSITIVE
        System.out.println(sentiment);
    }


    // 自定义java类输出
    public static void pojoOutput() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);
        Person person = extractor.extractPersonFrom("John Doe was born on July 4, 1968...");
        System.out.println(person);
    }
    // 聊天记忆
    public static void chatMemoryTest(){
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 不同用户有独立的对话记忆
        String answer1 = assistant.chat(1, "My name is Klaus");
        String answer2 = assistant.chat(2, "My name is Francine");
    }

    //工具测试
    public static void toolTest(){
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new Calculator())
                .build();

        String answer = assistant.chat("What is 1+2 and 3*4?");
    }

    // RAG
    public static void raggedTest(){
//        EmbeddingStore embeddingStore = "";
//        EmbeddingModel embeddingModel = ...;
//        ChatModel model = OpenAiChatModel.builder()
//                .apiKey(API_KEY)
//                .modelName("qwen-flash")
//                .baseUrl(BASE_URL)
//                .build();
//        ContentRetriever retriever = new EmbeddingStoreContentRetriever(
//                embeddingStore,
//                embeddingModel
//        );
//
//        Assistant assistant = AiServices.builder(Assistant.class)
//                .chatModel(model)
//                .contentRetriever(retriever)
//                .build();

    }
    //流式输出
    public static void streamOutputTest(){
        StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();

        StreamAssistant assistant = AiServices.create(StreamAssistant.class, streamingModel);

        TokenStream tokenStream = assistant.streamChat("Tell me a joke");
        tokenStream
                .onPartialResponse(System.out::print)
                .onCompleteResponse(response -> System.out.println("\\nDone!"))
                .onError(Throwable::printStackTrace)
                .start();
    }

    // 获取元数据
    public static void metaDataTest(){
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-flash")
                .baseUrl(BASE_URL)
                .build();
        MetaAssistant assistant = AiServices.create(MetaAssistant.class, model);
        Result<List<String>> result = assistant.generateOutlineFor("Java");

        List<String> outline = result.content();           // 内容
        TokenUsage tokenUsage = result.tokenUsage();       // Token使用量
        List<Content> sources = result.sources();          // RAG来源
        List<ToolExecution> tools = result.toolExecutions(); // 工具执行记录
        System.out.println(outline);
        System.out.println(tokenUsage);
        System.out.println(sources);
        System.out.println(tools);
    }
    // 将复杂逻辑拆分成多个AI Service
    interface GreetingDetector {
        @UserMessage("Is the following text a greeting? {{it}}")
        boolean isGreeting(String text);
    }

    interface ChatBot {
        @SystemMessage("You are a helpful assistant.")
        String reply(String message);
    }

    class Application {
        private final GreetingDetector detector;
        private final ChatBot chatBot;

        Application(GreetingDetector detector, ChatBot chatBot) {
            this.detector = detector;
            this.chatBot = chatBot;
        }

        public String handle(String message) {
            if (detector.isGreeting(message)) {
                return "Hello! How can I help you?";
            } else {
                return chatBot.reply(message);
            }
        }
    }
    // LLM会自动调用add(1,2)和multiply(3,4)，然后给出答案
    public static void main(String[] args) {
//        basicUse();
//        userSystemPrompt();
//        userTemplate();
//        structOutput();
//        enumOutput();
//        pojoOutput();
        metaDataTest();
    }
}
