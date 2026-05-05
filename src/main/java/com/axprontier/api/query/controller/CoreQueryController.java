package com.axprontier.api.query.controller;

import com.axprontier.api.global.apiPayload.ApiResponse;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import com.axprontier.api.query.service.CoreOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoreQueryController {

    private final CoreOrchestratorService coreOrchestratorService;

    public CoreQueryController(CoreOrchestratorService coreOrchestratorService) {
        this.coreOrchestratorService = coreOrchestratorService;
    }

    @PostMapping("/query")
    public ApiResponse<CoreQueryResponse> query(@Valid @RequestBody CoreQueryRequest request) {
        return ApiResponse.ok(coreOrchestratorService.query(request));
    }
}
