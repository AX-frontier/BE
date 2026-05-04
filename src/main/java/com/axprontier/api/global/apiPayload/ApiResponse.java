package com.axprontier.api.global.apiPayload;

import com.axprontier.api.global.apiPayload.code.BaseErrorCode;
import com.axprontier.api.global.apiPayload.code.BaseSuccessCode;
import com.axprontier.api.global.apiPayload.code.GeneralSuccessCode;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(T data) {
        return onSuccess(GeneralSuccessCode.OK, data);
    }

    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T data) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), data);
    }

    public static ApiResponse<Void> onFailure(BaseErrorCode code) {
        return onFailure(code, null);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T data) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), data);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, String message, T data) {
        return new ApiResponse<>(false, code.getCode(), message, data);
    }
}
