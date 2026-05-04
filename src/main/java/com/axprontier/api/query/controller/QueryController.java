package com.axprontier.api.query.controller;

import com.axprontier.api.global.apiPayload.ApiResponse;
import com.axprontier.api.query.dto.QueryCreateRequest;
import com.axprontier.api.query.dto.QueryCreateResponse;
import com.axprontier.api.query.service.QueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{conversationUid}/queries")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ApiResponse<QueryCreateResponse> create(
            @PathVariable UUID conversationUid,
            @Valid @RequestBody QueryCreateRequest request
    ) {
        return ApiResponse.ok(queryService.create(conversationUid, request));
    }
}
