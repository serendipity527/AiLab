package com.example.ailab.controller;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatModel openAiChatModel;
    ChatController(ChatModel chatModel) {
        this.openAiChatModel = chatModel;
    }

    @RequestMapping("/chat")
    public String chat(){
        return openAiChatModel.chat("你好");
    }
}
