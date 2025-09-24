package com.kapa.binance.base.exception;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.constants.MessageConstant;
import com.kapa.binance.base.response.BaseErrorResponse;
import com.kapa.binance.base.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ExceptionHandlers {

    @Value("${config.debug-enable:false}")
    private boolean debugEnable;
    private final ObjectMapper objectMapper;

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception ex) {
        log.error("Global exception {}", ex.getMessage());

        if (debugEnable) {
            return BaseResponse.debug(CodeConstant.CODE_500,
                    MessageConstant.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
        return BaseResponse.debug(CodeConstant.CODE_500, MessageConstant.INTERNAL_SERVER_ERROR, null);
    }

    @ExceptionHandler(Ex500.class)
    public BaseResponse<?> handleException(Ex500 ex) {
        log.error("Ex500 exception {}", ex.getMessage());

        if (debugEnable) {
            return BaseResponse.debug(ex.getCode(), ex.getMessage(), String.valueOf(ex.getError()));
        }
        return BaseResponse.debug(CodeConstant.CODE_500, MessageConstant.INTERNAL_SERVER_ERROR, null);
    }

    @ExceptionHandler(CustomException.class)
    public BaseResponse<?> handleCustomException(CustomException ex) {
        return buildErrorResponse(ex);
    }

    @ExceptionHandler(Ex400.class)
    public BaseResponse<?> handleEx400(Ex400 ex) {
        return buildErrorResponse(ex);
    }

    @ExceptionHandler(Ex401.class)
    public BaseResponse<?> handleEx401(Ex401 ex) {
        return buildErrorResponse(ex);
    }

    @ExceptionHandler(Ex403.class)
    public BaseResponse<?> handleEx403(Ex403 ex) {
        return BaseResponse.success(ex.getCode(), ex.getMessage(), ex.getError());
    }

//    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
//    public BaseResponse<?> handleEx403(org.springframework.security.access.AccessDeniedException ex) {
//        return BaseResponse.success(CodeConstant.CODE_403, ex.getMessage());
//    }

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public BaseResponse<?> handleUnauthorized(HttpClientErrorException.Unauthorized ex) throws JsonProcessingException {
        String errorMessage = ex.getResponseBodyAsString();
//        JsonNode rootNode = objectMapper.readTree(errorMessage);

//        String code = rootNode.path("code").asText();
//        Integer reasonCode = getErrorCode(code, errorMessage);
//        String uuid = SecurityUtils.getUuid();

//        if (uuid != null) {
//            userRepository.findByUuid(uuid).ifPresent(user -> {
//                user.setIsActive(false);
//                user.setReasonCode(reasonCode);
//                userRepository.save(user);
//            });
//        }
//
//        String msg = UserReasonEnum.getMessageByCode(reasonCode);
//        return BaseResponse.debug(CodeConstant.CODE_401, msg != null ? msg : MessageConstant.UNAUTHORIZED, errorMessage);
        return BaseResponse.debug(CodeConstant.CODE_401, MessageConstant.UNAUTHORIZED, errorMessage);
    }

//    private static Integer getErrorCode(String code, String errorMessage) {
//        if (StringUtils.isEmpty(code) && StringUtils.isEmpty(errorMessage)) {
//            return UserReasonEnum.API_KEY_PERMISSION_DENIED.getCode();
//        }
//
//        return switch (code) {
//            case "50101" -> UserReasonEnum.API_KEY_MISMATCH_ENV.getCode();
//            case "50105" -> UserReasonEnum.PASSPHRASE_INVALID.getCode();
//            case "50113" -> UserReasonEnum.SECRET_KEY_INVALID.getCode();
//            case "50119", "50111" -> UserReasonEnum.API_KEY_INVALID.getCode();
//            case "50110" -> UserReasonEnum.IP_NOT_SET_FOR_API_KEY.getCode();
//            default -> UserReasonEnum.ADMIN_LOCK_ERROR.getCode();
//        };
//    }


    @ExceptionHandler(Ex404.class)
    public BaseResponse<?> handleEx404(Ex404 ex) {
        return buildErrorResponse(ex);
    }

    @ExceptionHandler(Ex422.class)
    public BaseResponse<?> handleEx422(Ex422 ex) {
        return buildErrorResponse(ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public BaseResponse<?> handleEx422(NoResourceFoundException ex) {
        return BaseResponse.success(CodeConstant.CODE_404, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleException(MethodArgumentNotValidException ex) {
        List<BaseErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> BaseErrorResponse.builder()
                        .code(e.getCode())
                        .property(e.getField())
                        .message(e.getDefaultMessage())
                        .build()
                ).toList();
        return BaseResponse.success(CodeConstant.CODE_400, MessageConstant.BAD_REQUEST, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handleException(HttpMessageNotReadableException ex) {
        return BaseResponse.success(CodeConstant.CODE_400, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<?> handleException(MissingServletRequestParameterException ex) {
        return BaseResponse.success(CodeConstant.CODE_400, MessageConstant.BAD_REQUEST, BaseErrorResponse.builder()
                .parameterName(ex.getParameterName())
                .parameterType(ex.getParameterType())
                .build());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<?> handleException(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method Not Allowed {}", ex.getMethod());
        return BaseResponse.success(CodeConstant.CODE_405, MessageConstant.METHOD_NOT_ALLOWED);
    }

    private BaseResponse<?> buildErrorResponse(CustomException ex) {
        log.error("Handling exception: {}", ex.getMessage());

        String code = ex.getCode();
        String message = ex.getMessage();
        Object error = ex.getError();

        if (error == null) return BaseResponse.success(code, message);

        if (error instanceof BaseErrorResponse errorResponse) {
            errorResponse.setCode(null);
            errorResponse.setMessage(null);
            return BaseResponse.success(code, message, errorResponse);
        }

        if (error instanceof List<?> errorList) {
            if (!errorList.isEmpty() && errorList.get(0) instanceof BaseErrorResponse) {
                return BaseResponse.success(code, message, errorList);
            }
        }

        return BaseResponse.success(code, message);
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public BaseResponse<?> handleException(HttpClientErrorException.Forbidden ex) {
        log.warn("Forbidden {}", ex.getMessage());
        return BaseResponse.success(CodeConstant.CODE_403, MessageConstant.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public BaseResponse<?> handleException(AccessDeniedException ex) {
        log.warn("AccessDeniedException {}", ex.getMessage());
        return BaseResponse.success(CodeConstant.CODE_403, MessageConstant.FORBIDDEN);
    }
}
