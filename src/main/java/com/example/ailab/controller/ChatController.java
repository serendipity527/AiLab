package com.example.ailab.controller;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponse;
import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponseAndError;
@RestController
public class ChatController {
    private final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";


    private final ChatModel openAiChatModel;
    ChatController(ChatModel chatModel) {
        this.openAiChatModel = chatModel;
    }

    @RequestMapping("/chat")
    public String chat(){
        return openAiChatModel.chat("你好");
    }


    @RequestMapping("/streamChat")
    public String streamChat(){
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelName("qwen-flash")
                .build();
        String userMessage = "Tell me a joke";

//        model.chat(userMessage, new StreamingChatResponseHandler() {
//
//            @Override
//            public void onPartialResponse(String partialResponse) {
//                System.out.println("onPartialResponse: " + partialResponse);
//            }
//
//            @Override
//            public void onPartialThinking(PartialThinking partialThinking) {
//                System.out.println("onPartialThinking: " + partialThinking);
//            }
//
//            @Override
//            public void onPartialToolCall(PartialToolCall partialToolCall) {
//                System.out.println("onPartialToolCall: " + partialToolCall);
//            }
//
//            @Override
//            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
//                System.out.println("onCompleteToolCall: " + completeToolCall);
//            }
//
//            @Override
//            public void onCompleteResponse(ChatResponse completeResponse) {
//                System.out.println("onCompleteResponse: " + completeResponse);
//            }
//
//            @Override
//            public void onError(Throwable error) {
//                error.printStackTrace();
//            }
//        });


        model.chat("Tell me a joke", onPartialResponse(System.out::print));
        model.chat("Tell me a joke", onPartialResponseAndError(System.out::print, Throwable::printStackTrace));

        return model.toString();
    }
}
