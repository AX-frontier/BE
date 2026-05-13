package com.axprontier.api.query.controller;

import com.axprontier.api.global.apiPayload.ApiResponse;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import com.axprontier.api.query.service.CoreOrchestratorService;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryStream(@Valid @RequestBody CoreQueryRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> coreOrchestratorService.queryStream(request, emitter))
                .exceptionally(ex -> { emitter.completeWithError(ex); return null; });
        return emitter;
    }
}
