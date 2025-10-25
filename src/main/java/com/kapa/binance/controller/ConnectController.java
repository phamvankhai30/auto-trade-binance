package com.kapa.binance.controller;

import com.kapa.binance.base.response.BaseResponse;
import com.kapa.binance.model.request.ConnectRequest;
import com.kapa.binance.service.ConnectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/connect")
@RequiredArgsConstructor
public class ConnectController {
    private final ConnectService connectService;

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/open")
    public BaseResponse<?> openConnect(@RequestBody @Valid ConnectRequest request) {
        connectService.apiOpenConnect(request);
        return BaseResponse.success();
    }

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/close")
    public BaseResponse<?> closeConnect(@RequestBody @Valid ConnectRequest request) {
        connectService.apiCloseConnect(request.getUuid());
        return BaseResponse.success();
    }

    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/reset")
    public BaseResponse<?> resetConnect(@RequestBody @Valid ConnectRequest request) {
        connectService.apiResetConnect(request);
        return BaseResponse.success();
    }
}
