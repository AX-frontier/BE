package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.TargetAgent;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedAgentRouter {

    private static final List<String> DOCUMENT_REVIEW_KEYWORDS = List.of(
            "문서", "전자결재", "공문", "검토", "수정", "두문", "본문", "결문", "맞춤법", "문장"
    );
    private static final List<String> LIBRARY_KEYWORDS = List.of(
            "도서관", "학술정보관", "도서", "책", "대출", "반납", "열람실", "좌석", "서가", "청구기호", "자료실", "추천"
    );
    private static final List<String> MAIN_KEYWORDS = List.of(
            "학교", "공지", "학사", "수강신청", "장학", "졸업", "복수전공", "부전공", "휴학", "복학", "등록금", "학사일정"
    );

    public TargetAgent route(String message) {
        String normalizedMessage = message == null ? "" : message.replaceAll("\\s+", "");

        if (containsAny(normalizedMessage, DOCUMENT_REVIEW_KEYWORDS)) {
            return TargetAgent.DOCUMENT_REVIEW;
        }
        if (containsAny(normalizedMessage, LIBRARY_KEYWORDS)) {
            return TargetAgent.LIBRARY;
        }
        if (containsAny(normalizedMessage, MAIN_KEYWORDS)) {
            return TargetAgent.MAIN;
        }
        return TargetAgent.FALLBACK;
    }

    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }
}
