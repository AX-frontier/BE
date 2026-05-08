package com.axprontier.api.crawler.exception;

import com.axprontier.api.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CrawlerErrorCode implements BaseErrorCode {
    CRAWLER_API_FAILED(HttpStatus.BAD_GATEWAY, "CRAWLER502_1", "크롤링 API 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
