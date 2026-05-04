package com.axprontier.api.global.response;

public enum SuccessCode implements ResponseCode {
    OK("COMMON_200", "요청이 성공했습니다.");

    private final String code;
    private final String message;

    SuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
