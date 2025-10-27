package com.example.ailab.AiService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

// 第1步：定义接口
interface Assistant {
    String chat(String userMessage);
}
interface Friend {
    @SystemMessage("You are a good friend of mine. Answer using slang.")
    String chat(String userMessage);
}

public class AiServiceTest {
    private static final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    // 基本使用
    public static void basicUse(){
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

    public static void userSystemPrompt(){
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
    public  static void main(String[] args) {
        userSystemPrompt();
    }
}
