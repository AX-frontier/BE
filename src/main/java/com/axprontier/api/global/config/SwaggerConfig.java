package com.axprontier.api.global.config;

import com.axprontier.api.global.annotation.ApiErrorCodeExample;
import com.axprontier.api.global.annotation.ApiErrorCodeExamples;
import com.axprontier.api.global.apiPayload.code.BaseErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AX Prontier API")
                        .description("메타 에이전트 기반 대학 정보 통합 AI 서비스 API")
                        .version("v1")
                        .license(new License().name("AX Prontier")));
    }

    @Bean
    public OperationCustomizer errorCodeExampleCustomizer() {
        return (operation, handlerMethod) -> {
            ApiErrorCodeExamples examples = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);
            if (examples != null) {
                for (ApiErrorCodeExample example : examples.value()) {
                    addErrorCodeExample(operation, example.value(), example.name());
                }
                return operation;
            }

            ApiErrorCodeExample example = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);
            if (example != null) {
                addErrorCodeExample(operation, example.value(), example.name());
            }

            return operation;
        };
    }

    private void addErrorCodeExample(Operation operation, Class<? extends Enum<?>> enumClass, String name) {
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.name().equals(name) && constant instanceof BaseErrorCode errorCode) {
                addErrorResponse(operation, errorCode);
                return;
            }
        }
    }

    private void addErrorResponse(Operation operation, BaseErrorCode errorCode) {
        String httpStatusCode = String.valueOf(errorCode.getStatus().value());
        String exampleJson = String.format("""
                {
                  "success": false,
                  "code": "%s",
                  "message": "%s",
                  "data": null
                }
                """, errorCode.getCode(), errorCode.getMessage());

        io.swagger.v3.oas.models.responses.ApiResponse apiResponse =
                operation.getResponses().computeIfAbsent(httpStatusCode, statusCode ->
                        new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description(errorCode.getMessage())
                                .content(new Content()));

        MediaType mediaType = apiResponse.getContent()
                .computeIfAbsent("application/json", ignored -> new MediaType());

        mediaType.addExamples(errorCode.getCode(), new Example().value(exampleJson));
    }
}
