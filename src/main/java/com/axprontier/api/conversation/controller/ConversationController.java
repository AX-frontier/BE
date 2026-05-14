package com.axprontier.api.conversation.controller;

import com.axprontier.api.conversation.dto.ConversationCreateRequest;
import com.axprontier.api.conversation.dto.ConversationDetailResponse;
import com.axprontier.api.conversation.dto.ConversationListItemResponse;
import com.axprontier.api.conversation.dto.ConversationResponse;
import com.axprontier.api.conversation.service.ConversationService;
import com.axprontier.api.global.apiPayload.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ApiResponse<List<ConversationListItemResponse>> list(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(conversationService.list(userId, page, size));
    }

    @GetMapping("/{conversationUid}")
    public ApiResponse<ConversationDetailResponse> getDetail(
            @PathVariable UUID conversationUid,
            @RequestParam String userId
    ) {
        return ApiResponse.ok(conversationService.getDetail(conversationUid, userId));
    }
}
