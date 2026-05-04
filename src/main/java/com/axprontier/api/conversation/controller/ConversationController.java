package com.axprontier.api.conversation.controller;

import com.axprontier.api.conversation.dto.ConversationCreateRequest;
import com.axprontier.api.conversation.dto.ConversationResponse;
import com.axprontier.api.conversation.service.ConversationService;
import com.axprontier.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ApiResponse<ConversationResponse> create(@RequestBody ConversationCreateRequest request) {
        return ApiResponse.ok(conversationService.create(request));
    }
}
