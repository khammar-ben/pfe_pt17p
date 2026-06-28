package com.example.demo.web;

import com.example.demo.service.ChatbotService;
import com.example.demo.service.ChatbotService.ChatReply;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    private final ChatbotService chatbot;

    public ChatbotController(ChatbotService chatbot) {
        this.chatbot = chatbot;
    }

    @PostMapping
    ChatReply ask(@RequestBody ChatRequest request, Authentication authentication) {
        return chatbot.answer(request.message(), authentication);
    }

    record ChatRequest(@NotBlank String message) {
    }
}
