package com.kapa.binance.model.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@Getter
@Setter
@NoArgsConstructor
public class UserConnectionContext {
    private WebSocketSession session;
    private ScheduledFuture<?> renewTask;
    private ScheduledExecutorService scheduler;
    private WebSocketClient wsClient;
    private WebSocketHandler wsHandler;
    private Thread connectionThread;
    private String apiKey;
    private String listenKey;
    private String uuid;
    private Integer countRetryKey;
    private final Deque<Instant> connectTimestamps = new ArrayDeque<>();

    public void cleanup() {
        stopRenewTask();
        shutdownScheduler();
        closeSession();
        stopConnectionThread();
    }

    private void stopRenewTask() {
        if (renewTask != null && !renewTask.isCancelled()) {
            renewTask.cancel(true);
        }
        renewTask = null;
    }

    private void shutdownScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }

    public void closeSession() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            // log nếu cần
        }
    }

    private void stopConnectionThread() {
        try {
            if (connectionThread != null && connectionThread.isAlive()) {
                connectionThread.interrupt();
            }
        } catch (Exception e) {
            // log nếu cần
        }
    }

    public boolean canReconnect() {
        Instant now = Instant.now();

        // Xoá các lần reconnect cũ hơn 15s
        while (!connectTimestamps.isEmpty() &&
                Duration.between(connectTimestamps.peekFirst(), now).getSeconds() > 60) {
            connectTimestamps.pollFirst();
        }

        return connectTimestamps.size() < 3;
    }

    public boolean shouldDelayReconnect() {
        // Nếu đã có 1 reconnect trong 15s rồi thì lần này delay 5s
        return connectTimestamps.size() > 1;
    }

    public void increaseReconnect() {
        connectTimestamps.addLast(Instant.now());
    }


}
