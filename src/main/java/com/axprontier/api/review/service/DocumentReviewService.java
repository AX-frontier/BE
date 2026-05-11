package com.axprontier.api.review.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.query.dto.CoreQueryResponse;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.review.dto.DocumentReviewRequest;
import com.axprontier.api.review.entity.ReviewFinding;
import com.axprontier.api.review.entity.ReviewRequest;
import com.axprontier.api.review.entity.ReviewResult;
import com.axprontier.api.review.repository.ReviewFindingRepository;
import com.axprontier.api.review.repository.ReviewRequestRepository;
import com.axprontier.api.review.repository.ReviewResultRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DocumentReviewService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReviewService.class);

    private final AiGatewayService aiGatewayService;
    private final ConversationRepository conversationRepository;
    private final QueryRepository queryRepository;
    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final TransactionTemplate transactionTemplate;

    public DocumentReviewService(
            AiGatewayService aiGatewayService,
            ConversationRepository conversationRepository,
            QueryRepository queryRepository,
            ReviewRequestRepository reviewRequestRepository,
            ReviewResultRepository reviewResultRepository,
            ReviewFindingRepository reviewFindingRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.aiGatewayService = aiGatewayService;
        this.conversationRepository = conversationRepository;
        this.queryRepository = queryRepository;
        this.reviewRequestRepository = reviewRequestRepository;
        this.reviewResultRepository = reviewResultRepository;
        this.reviewFindingRepository = reviewFindingRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public CoreQueryResponse review(DocumentReviewRequest request) {
        Instant startedAt = Instant.now();
        Query query = transactionTemplate.execute(status -> saveReviewQuery(request));
        OrchestrateRequest aiRequest = new OrchestrateRequest(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message(),
                request.document()
        );

        try {
            OrchestrateResponse response = aiGatewayService.chat(TargetAgent.DOCUMENT_REVIEW, aiRequest);
            transactionTemplate.executeWithoutResult(status -> saveReviewArtifacts(query, request, response));
            logResult(request, response, "COMPLETED", startedAt);
            return toCoreResponse(response);
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(
                    TargetAgent.DOCUMENT_REVIEW.name(),
                    "DOCUMENT_REVIEW_AGENT_CALL_FAILED"
            );
            transactionTemplate.executeWithoutResult(status -> saveReviewArtifacts(query, request, fallbackResponse));
            logResult(request, fallbackResponse, "FAILED", startedAt);
            return toCoreResponse(fallbackResponse);
        }
    }

    private Query saveReviewQuery(DocumentReviewRequest request) {
        Conversation conversation = conversationRepository.findByConversationUid(request.conversationUid())
                .orElseGet(() -> conversationRepository.save(new Conversation(request.conversationUid(), "전자결재 문서검토")));
        return queryRepository.findByQueryUid(request.queryUid())
                .orElseGet(() -> queryRepository.save(
                        new Query(request.queryUid(), conversation, request.message(), "DOCUMENT_REVIEW")
                ));
    }

    private void saveReviewArtifacts(Query query, DocumentReviewRequest request, OrchestrateResponse response) {
        Map<String, Object> document = request.document();
        ReviewRequest reviewRequest = reviewRequestRepository.save(
                new ReviewRequest(
                        query,
                        stringValue(document.get("title"), "전자결재 문서"),
                        stringValue(document.get("docType"), "OFFICIAL_DOCUMENT"),
                        stringValue(document.get("bodyText"), "")
                )
        );
        ReviewResult reviewResult = reviewResultRepository.save(
                new ReviewResult(
                        reviewRequest,
                        response.confidence(),
                        response.reviewMarkdown() == null ? response.answer() : response.reviewMarkdown()
                )
        );
        if (response.findings() == null) {
            return;
        }
        for (Map<String, Object> finding : response.findings()) {
            reviewFindingRepository.save(
                    new ReviewFinding(
                            reviewResult,
                            stringValue(finding.get("category"), "문서검토"),
                            stringValue(finding.get("severity"), null),
                            buildFindingMessage(finding),
                            stringValue(finding.get("suggestedText"), null),
                            integerValue(finding.get("lineStart")),
                            integerValue(finding.get("lineEnd"))
                    )
            );
        }
    }

    private String buildFindingMessage(Map<String, Object> finding) {
        String originalText = stringValue(finding.get("originalText"), "");
        String reason = stringValue(finding.get("reason"), "");
        if (originalText.isBlank()) {
            return reason.isBlank() ? "문서검토 수정 제안" : reason;
        }
        if (reason.isBlank()) {
            return originalText;
        }
        return "원문: " + originalText + "\n사유: " + reason;
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void logResult(
            DocumentReviewRequest request,
            OrchestrateResponse response,
            String status,
            Instant startedAt
    ) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "document_review queryUid={} traceId={} conversationUid={} status={} latencyMs={} fallbackUsed={}",
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                status,
                latencyMs,
                response.fallbackUsed()
        );
    }

    private CoreQueryResponse toCoreResponse(OrchestrateResponse response) {
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
                response.matchedBooks(),
                response.summary(),
                response.findings(),
                response.criterionResults(),
                response.checkRequiredItems(),
                response.formatNoticeItems(),
                response.extractedTables(),
                response.revisedDocument(),
                response.reviewMarkdown(),
                false,
                null
        );
    }
}
