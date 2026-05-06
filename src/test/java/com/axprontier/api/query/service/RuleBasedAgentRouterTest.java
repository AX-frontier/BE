package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.axprontier.api.ai.dto.TargetAgent;
import org.junit.jupiter.api.Test;

class RuleBasedAgentRouterTest {

    private final RuleBasedAgentRouter router = new RuleBasedAgentRouter();

    @Test
    void routesMainKeywords() {
        assertThat(router.route("복수전공 신청 기간 알려줘")).isEqualTo(TargetAgent.MAIN);
        assertThat(router.route("수강신청 정정 기간 언제야?")).isEqualTo(TargetAgent.MAIN);
    }

    @Test
    void routesLibraryKeywords() {
        assertThat(router.route("파이썬 책 어디 있어?")).isEqualTo(TargetAgent.LIBRARY);
        assertThat(router.route("도서관 운영 시간 알려줘")).isEqualTo(TargetAgent.LIBRARY);
    }

    @Test
    void routesDocumentReviewKeywordsBeforeOtherAgents() {
        assertThat(router.route("이 공문 검토해줘")).isEqualTo(TargetAgent.DOCUMENT_REVIEW);
        assertThat(router.route("도서관 공문 문장 수정해줘")).isEqualTo(TargetAgent.DOCUMENT_REVIEW);
    }

    @Test
    void routesFallbackWhenNoKeywordMatches() {
        assertThat(router.route("안녕")).isEqualTo(TargetAgent.FALLBACK);
    }
}
