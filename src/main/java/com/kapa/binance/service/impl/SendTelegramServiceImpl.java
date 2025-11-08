package com.kapa.binance.service.impl;

import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.response.DataOrder;
import com.kapa.binance.service.SendTelegramService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SendTelegramServiceImpl implements SendTelegramService {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    @Value("${config.telegram.bot-token}")
    private String botToken;

    @Value("${config.telegram.chat-id}")
    private String chatId;

    @Override
    public void sendMessage(String message) {
        CompletableFuture.runAsync(() -> {
            try {
                doSendMessage(botToken, chatId, message);
            } catch (Exception e) {
                log.warn("Failed to send Telegram message: {}", e.getMessage());
            }
        });
    }

    @Override
    public void sendDisconnectMessage(AuthRequest authRequest) {
        final String fullName = authRequest.getFullName() != null
                ? authRequest.getFullName()
                : authRequest.getUuid();

        String message = """
                ⚠️ <b>Disconnect Alert</b> ⚠️
                User: %s
                """.formatted(fullName);

        sendMessage(message);
    }

    @Override
    public void sendTakeProfitMessage(AuthRequest authRequest, DataOrder order) {
        final String fullName = authRequest.getFullName() != null
                ? authRequest.getFullName()
                : authRequest.getUuid();

        Double realizedProfit = order.getRealizedProfit();
        if (realizedProfit == null || realizedProfit == 0) {
            log.debug("Skipped sending Take Profit message with zero profit");
            return;
        }

        String message = """
                💰 <b>Take Profit Alert</b> 💰
                User: %s
                Symbol: %s
                Position Side: %s
                Take Profit: %s
                """.formatted(
                fullName,
                order.getSymbol(),
                order.getPositionSide(),
                realizedProfit
        );

        sendMessage(message);
    }

    private void doSendMessage(String token, String chatId, String text) {
        if (text == null || text.isBlank()) {
            log.debug("Skipped sending empty Telegram message");
            return;
        }

        String url = "https://api.telegram.org/bot" + token + "/sendMessage";
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

        String params = "chat_id=" + chatId +
                "&text=" + encodedText +
                "&parse_mode=HTML";

        RequestBody body = RequestBody.create(params, FORM);
        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Telegram send failed: {}", response.body() != null ? response.body().string() : "no response");
            }
        } catch (IOException e) {
            log.warn("Telegram request error: {}", e.getMessage());
        }
    }
}
