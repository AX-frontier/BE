package com.axprontier.api.global.error;

import com.axprontier.api.global.response.ResponseCode;
import org.springframework.http.HttpStatus;

public interface ErrorResponseCode extends ResponseCode {

    HttpStatus getStatus();
}
