package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CoreOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(CoreOrchestratorService.class);

    private final RuleBasedAgentRouter ruleBasedAgentRouter;
    private final AiGatewayService aiGatewayService;

    public CoreOrchestratorService(RuleBasedAgentRouter ruleBasedAgentRouter, AiGatewayService aiGatewayService) {
        this.ruleBasedAgentRouter = ruleBasedAgentRouter;
        this.aiGatewayService = aiGatewayService;
    }

    public CoreQueryResponse query(CoreQueryRequest request) {
        TargetAgent targetAgent = ruleBasedAgentRouter.route(request.message());
        Instant startedAt = Instant.now();

        try {
            OrchestrateResponse response = aiGatewayService.chat(targetAgent, new OrchestrateRequest(
                    request.queryUid(),
                    request.traceId(),
                    request.conversationUid(),
                    request.message()
            ));
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info(
                    "core_orchestrator queryUid={} traceId={} conversationUid={} targetAgent={} intent={} status={} latencyMs={} fallbackUsed={}",
                    request.queryUid(),
                    request.traceId(),
                    request.conversationUid(),
                    response.targetAgent(),
                    response.intent(),
                    "COMPLETED",
                    latencyMs,
                    response.fallbackUsed()
            );

            return new CoreQueryResponse(
                    response.targetAgent(),
                    response.intent(),
                    response.answer(),
                    response.sources(),
                    response.confidence(),
                    response.fallbackUsed(),
                    response.fallbackReason(),
                    response.searchKeyword(),
                    response.resultCount(),
                    response.matchedBooks()
            );
        } catch (RuntimeException exception) {
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info(
                    "core_orchestrator queryUid={} traceId={} conversationUid={} targetAgent={} intent={} status={} latencyMs={} fallbackUsed={}",
                    request.queryUid(),
                    request.traceId(),
                    request.conversationUid(),
                    targetAgent,
                    "-",
                    "FAILED",
                    latencyMs,
                    targetAgent == TargetAgent.FALLBACK
            );
            throw exception;
        }
    }
}
