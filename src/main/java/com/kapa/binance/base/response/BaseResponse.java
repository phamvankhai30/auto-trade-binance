package com.kapa.binance.base.response;

import ch.qos.logback.core.util.StringUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kapa.binance.base.constants.CodeConstant;
import com.kapa.binance.base.constants.MessageConstant;
import com.kapa.binance.base.request.BasePageRequest;
import lombok.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "message", "debug", "responseId", "serverTime", "pagination", "data"})
public class BaseResponse<TResponse> {
    private String code;
    private String message;
    private String debug;
    private String responseId;
    private Date serverTime;
    private BasePageResponse pagination;
    private TResponse data;

    public static <TResponse> BaseResponse<TResponse> success() {
        return baseResponseBuilder(CodeConstant.CODE_200, MessageConstant.SUCCESS);
    }

    public static <TResponse> BaseResponse<TResponse> success(TResponse data) {
        return baseResponseBuilder(CodeConstant.CODE_200, MessageConstant.SUCCESS, data);
    }

    public static <TResponse> BaseResponse<TResponse> success(String code, String message) {
        return baseResponseBuilder(code, message);
    }

    public static <TResponse> BaseResponse<TResponse> success(String code, String message, TResponse data) {
        return baseResponseBuilder(code, message, data);
    }

    public static <TResponse> BaseResponse<TResponse> page(BasePageResponse pagination, TResponse data) {
        return baseResponseBuilder(pagination, data);
    }

    public static <TResponse> BaseResponse<TResponse> page(int page, int size, long total, TResponse data) {
        return baseResponseBuilder(BasePageResponse.of(page, size, total), data);
    }

    public static <TResponse> BaseResponse<TResponse> page(BasePageRequest pageRequest, long total, TResponse data) {
        return baseResponseBuilder(BasePageResponse.of(pageRequest, total), data);
    }

    public static <TResponse> BaseResponse<TResponse> page(Page<?> page, TResponse data) {
        Pageable pageable = page.getPageable();
        return baseResponseBuilder(BasePageResponse.of(pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements()), data);
    }

    public static <TResponse> BaseResponse<List<TResponse>> subList(Integer page, Integer size, List<TResponse> data) {
        if (data == null || data.isEmpty()) {
            return BaseResponse.<List<TResponse>>builder()
                    .code(CodeConstant.CODE_200)
                    .message(MessageConstant.SUCCESS)
                    .responseId(UUID.randomUUID().toString())
                    .serverTime(new Date())
                    .pagination(BasePageResponse.of(page, size, 0))
                    .data(Collections.emptyList())
                    .build();
        }

        int totalElements = data.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<TResponse> sublist = (start < totalElements) ? data.subList(start, end) : Collections.emptyList();

        return BaseResponse.<List<TResponse>>builder()
                .code(CodeConstant.CODE_200)
                .message(MessageConstant.SUCCESS)
                .responseId(UUID.randomUUID().toString())
                .serverTime(new Date())
                .pagination(BasePageResponse.of(page, size, totalElements))
                .data(sublist)
                .build();
    }


    public static <TResponse> BaseResponse<TResponse> debug(String code, String message,
                                                            String messageDetail) {
        return baseResponseBuilder(code, message, messageDetail);
    }

    private static <TResponse> BaseResponse<TResponse> baseResponseBuilder(BasePageResponse pagination, TResponse data) {
        return BaseResponse.<TResponse>builder()
                .code(CodeConstant.CODE_200)
                .message(MessageConstant.SUCCESS)
                .responseId(UUID.randomUUID().toString())
                .serverTime(new Date())
                .pagination(pagination)
                .data(data)
                .build();
    }

    private static <TResponse> BaseResponse<TResponse> baseResponseBuilder(String code, String message, TResponse data) {
        return BaseResponse.<TResponse>builder()
                .code(code)
                .message(message)
                .responseId(UUID.randomUUID().toString())
                .serverTime(new Date())
                .data(data)
                .build();
    }

    private static <TResponse> BaseResponse<TResponse> baseResponseBuilder(String code, String message) {
        return BaseResponse.<TResponse>builder()
                .code(code)
                .message(message)
                .responseId(UUID.randomUUID().toString())
                .serverTime(new Date())
                .build();
    }

    private static <TResponse> BaseResponse<TResponse> baseResponseBuilder(String code, String message,
                                                                           String messageDebug) {
        return BaseResponse.<TResponse>builder()
                .code(code)
                .message(message)
                .debug(messageDebug)
                .responseId(UUID.randomUUID().toString())
                .serverTime(new Date())
                .build();
    }

    public static ResponseEntity<?> file(FileResponse fileResponse) {
        if (fileResponse == null ||
                StringUtil.isNullOrEmpty(fileResponse.getFileName()) ||
                fileResponse.getFileData() == null) {
            throw new IllegalArgumentException("fileResponse, fileName,  and fileData must not be null");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileResponse.getFileName()
                )
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .body(new ByteArrayResource(fileResponse.getFileData()));
    }
}
