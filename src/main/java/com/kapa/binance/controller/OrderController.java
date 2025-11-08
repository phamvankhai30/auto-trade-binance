package com.kapa.binance.controller;

import com.kapa.binance.base.exception.Ex400;
import com.kapa.binance.base.response.BaseResponse;
import com.kapa.binance.base.utils.StringUtil;
import com.kapa.binance.constant.CommonConstant;
import com.kapa.binance.entity.OrderEntity;
import com.kapa.binance.entity.StepEntity;
import com.kapa.binance.entity.StepSymbolEntity;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.PositionInfo;
import com.kapa.binance.repository.OrderRepository;
import com.kapa.binance.repository.StepRepository;
import com.kapa.binance.repository.StepSymbolRepository;
import com.kapa.binance.service.UserService;
import com.kapa.binance.service.external.OrderApi;
import com.kapa.binance.service.external.PositionApi;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApi orderApi;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final PositionApi positionApi;
    private final StepSymbolRepository stepSymbolRepository;
    private final StepRepository stepRepository;


    @PreAuthorize("@authService.authorize('ADMIN')")
    @PostMapping("/retry-dca")
    public BaseResponse<?> retryDca(@RequestParam @NotNull Long orderId) {

        // 1️⃣ Kiểm tra đơn hàng tồn tại
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Ex400("Order not found with id: " + orderId));

        if (StringUtils.isBlank(order.getUuid())) {
            throw new Ex400("Order with id: " + orderId + " does not have a valid UUID.");
        }

        // 2️⃣ Lấy thông tin user và position
        AuthRequest auth = userService.getUserAuthByUuidWithError(order.getUuid());
        PositionInfo positionInfo = positionApi.getPosition(auth, order.getSymbol(), order.getPosSide());

        if (positionInfo == null || positionInfo.getPositionAmt() == 0) {
            throw new Ex400("Cannot retry DCA for order id: " + orderId + " because there is no open position.");
        }

        // 3️⃣ Chuẩn bị dữ liệu cho step kế tiếp
        int nextStep = order.getStep() + 1;
        String randomId = StringUtil.random(CommonConstant.DA);
        String symbol = order.getSymbol();
        String posSide = order.getPosSide();
        Double entryPrice = positionInfo.getEntryPrice();
        Double roi;
        Double usdt;
        Integer leverage;

        // 4️⃣ Lấy cấu hình step (ưu tiên StepSymbolEntity)
        StepSymbolEntity stepSymbol = stepSymbolRepository.findFirstByUuidAndSymbolAndStep(order.getUuid(), symbol, nextStep);
        if (stepSymbol != null) {
            roi = stepSymbol.getRoi();
            usdt = stepSymbol.getUsdt();
            leverage = stepSymbol.getLever();
        } else {
            StepEntity stepEntity = stepRepository.findFirstByUuidAndStep(order.getUuid(), nextStep);
            if (stepEntity == null) {
                throw new Ex400("No step configuration found for retrying DCA for order id: " + orderId);
            }
            roi = stepEntity.getRoi();
            usdt = stepEntity.getUsdt();
            leverage = stepEntity.getLever();
        }

        // 5️⃣ Gọi retry DCA
        orderApi.retryDca(auth, symbol, posSide, entryPrice, roi, leverage, usdt, randomId);

        // 6️⃣ Tạo bản ghi Order mới
        OrderEntity newOrder = new OrderEntity();
        newOrder.setClientIdParent(order.getClientIdChildren());
        newOrder.setClientIdChildren(randomId);
        newOrder.setPosSide(posSide);
        newOrder.setSymbol(symbol);
        newOrder.setStep(nextStep);
//        newOrder.setMgnMode(order.getMgnMode());
        newOrder.setIsNew(false);
        newOrder.setIsEndStep(false);
        newOrder.setVolume(order.getVolume());
        newOrder.setUuid(auth.getUuid());

        Date now = new Date();
        newOrder.setCreateAt(now);
        newOrder.setUpdatedAt(now);
        newOrder.setCTime(now.getTime());

        orderRepository.save(newOrder);

        return BaseResponse.success();
    }

}
