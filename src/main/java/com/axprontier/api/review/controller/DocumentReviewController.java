package com.axprontier.api.review.controller;

import com.axprontier.api.global.apiPayload.ApiResponse;
import com.axprontier.api.query.dto.CoreQueryResponse;
import com.axprontier.api.review.dto.DocumentReviewRequest;
import com.axprontier.api.review.service.DocumentReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentReviewController {

    private final DocumentReviewService documentReviewService;

    public DocumentReviewController(DocumentReviewService documentReviewService) {
        this.documentReviewService = documentReviewService;
    }

    @PostMapping("/document-review")
    public ApiResponse<CoreQueryResponse> review(@Valid @RequestBody DocumentReviewRequest request) {
        return ApiResponse.ok(documentReviewService.review(request));
    }
}
