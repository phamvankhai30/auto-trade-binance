package com.kapa.binance.service.impl;

import com.kapa.binance.base.exception.Ex422;
import com.kapa.binance.base.exception.Ex500;
import com.kapa.binance.config.EnvConfig;
import com.kapa.binance.enums.ConnectedEnum;
import com.kapa.binance.model.dtos.AuthRequest;
import com.kapa.binance.model.dtos.UserConnectionContext;
import com.kapa.binance.model.request.ConnectRequest;
import com.kapa.binance.model.response.ConnectionResponse;
import com.kapa.binance.service.ConnectService;
import com.kapa.binance.service.OrderService;
import com.kapa.binance.service.SendTelegramService;
import com.kapa.binance.service.UserService;
import com.kapa.binance.service.external.ListenKeyApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectServiceImpl implements ConnectService, DisposableBean {

    private static final String KEY_LOG = "userId";
    private static final int RENEW_INTERVAL_MINUTES = 50;
    private static final int ERROR_DELAY_SECONDS = 3;

    private final Map<String, UserConnectionContext> userContexts = new ConcurrentHashMap<>();
    private final EnvConfig envConfig;
    private final ListenKeyApi listenKeyApi;
    private final OrderService orderService;
    private final UserService userService;
    private final SendTelegramService sendTelegramService;

    public static void withMDC(String value, Runnable task) {
        MDC.put(KEY_LOG, value);
        try {
            task.run();
        } finally {
            MDC.remove(KEY_LOG);
        }
    }

    @Override
    public void destroy() {
        List<String> uuidList = new ArrayList<>(userContexts.keySet());
        userService.updateStatusConnectSync(uuidList, ConnectedEnum.SERVICE_UNAVAILABLE.name());
        log.info("All user close size: {}", uuidList.size());
    }

    @Override
    public void apiOpenConnect(ConnectRequest request) {
        String uuid = request.getUuid();
        AuthRequest auth = userService.getUserAuthByUuid(uuid);

        if (auth == null) throw new Ex422("User not found");
        if (!auth.getIsActive()) throw new Ex422("User not active");
        if (userContexts.containsKey(uuid)) throw new Ex422("User already connected");

        try {
            ConnectionResponse result = openConnect(auth, false).get(3, TimeUnit.SECONDS);
            if (result == null) {
                throw new Ex422("Connection failed");
            }

            if (result.getCode() != 200) {
                throw new Ex422(result.getMsg());
            }
        } catch (Ex422 ex422) {
            throw ex422;
        } catch (Exception e) {
            log.error("Connection failed for user {}: {}", uuid, e.getMessage());
            throw new Ex500("WebSocket connection failed");
        }
    }

    @Override
    public void apiCloseConnect(String uuid) {
        UserConnectionContext userConnectionContext = userContexts.get(uuid);
        if (userConnectionContext != null) {
            userConnectionContext.closeSession();
            userContexts.remove(uuid);
            log.info("Close connect uuid: {}", uuid);
        } else {
            log.warn("No active connection found for uuid: {}", uuid);
            throw new Ex422("Not found user connect: " + uuid);
        }
    }

    @Override
    public void apiResetConnect(ConnectRequest request) {
        String uuid = request.getUuid();
        log.info("Start reset leader uuid: {}", uuid);
        apiCloseConnect(uuid);
        apiOpenConnect(request);
        log.info("End reset leader uuid: {}", uuid);
    }

    @Override
    public CompletableFuture<ConnectionResponse> openConnect(AuthRequest authRequest, boolean isReconnect) {
        String uuid = authRequest.getUuid();
        CompletableFuture<ConnectionResponse> future = new CompletableFuture<>();

        withMDC(uuid, () -> {
            log.info("1. Open connect uuid: {} isReconnect: {}", uuid, isReconnect);

            if (userContexts.containsKey(uuid) && !isReconnect) {
                log.warn("Connection already exists for uuid: {}", uuid);
                future.complete(ConnectionResponse.of(409, "Connection already exists"));
                return;
            }

            if (isReconnect) {
                UserConnectionContext existingContext = userContexts.get(uuid);
                if (existingContext != null) {
                    existingContext.cleanup();
                    String listenKey = listenKeyApi.create(existingContext.getApiKey());
                    existingContext.setListenKey(listenKey);

                    log.info("Cleaned up existing context for reconnect, uuid: {}", uuid);
                    startConnectionThread(existingContext);
                }
            } else {
                UserConnectionContext context = createUserContext(authRequest, future);
                if (context != null) {
                    startConnectionThread(context);
                } else {
                    log.error("Failed to initialize UserConnectionContext");
                }
            }
        });
        return future;
    }

    private UserConnectionContext createUserContext(AuthRequest authRequest, CompletableFuture<ConnectionResponse> future) {
        try {
            String uuid = authRequest.getUuid();
            String apiKey = authRequest.getApiKey();

            String listenKey = listenKeyApi.create(apiKey);
            if (listenKey == null) {
                future.complete(ConnectionResponse.of(500, "Failed to create listenKey"));
                return null;
            }

            UserConnectionContext context = new UserConnectionContext();
            context.setUuid(uuid);
            context.setApiKey(apiKey);
            context.setListenKey(listenKey);
            context.setWsHandler(createWebSocketHandler(authRequest, future));

            return context;
        } catch (HttpClientErrorException e) {
            log.error("Req createUserContext error: code={} message={}", e.getStatusCode().value(), e.getMessage());
            future.complete(ConnectionResponse.of(e.getStatusCode().value(), e.getMessage()));
        } catch (Exception e) {
            future.complete(ConnectionResponse.of(500, "Failed to create listenKey"));
        }

        return null;
    }

    private WebSocketHandler createWebSocketHandler(AuthRequest authRequest, CompletableFuture<ConnectionResponse> future) {
        String uuid = authRequest.getUuid();

        return new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                withMDC(uuid, () -> {
                    log.info("3. WebSocket connected: status={} session={}", session.isOpen(), session.getId());
                    UserConnectionContext context = userContexts.get(uuid);
                    if (session.isOpen() && context != null) {
                        context.setSession(session);
                        startRenewTask(context);
                        userContexts.put(uuid, context);
                        userService.updateStatusConnectAsync(List.of(uuid), ConnectedEnum.CONNECTED.name());
                        future.complete(ConnectionResponse.of(200, "Connection successful"));
                    } else {
                        log.error("afterConnectionEstablished failed");
                    }
                });
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                withMDC(uuid, () -> {
//                    log.info("Received {}", message.getPayload());
                    orderService.receiveMessage(authRequest, message.getPayload().toString());
                });
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                withMDC(uuid, () -> log.error("WebSocket error: {}", exception.getMessage(), exception));
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                withMDC(uuid, () -> {
                    UserConnectionContext context = userContexts.get(uuid);
                    int code = status.getCode();

                    boolean isNormalClose = code == CloseStatus.NORMAL.getCode()
                            || code == CloseStatus.GOING_AWAY.getCode();

                    if (isNormalClose) {
                        log.info("WebSocket closed normally. uuid={}, session={}, status={}", uuid, session.getId(), status);
                        clearUser(authRequest); // đóng bình thường thì dọn dẹp tài nguyên
                        return;
                    }

                    log.warn("WebSocket closed unexpectedly. uuid={}, session={}, status={}", uuid, session.getId(), status);

                    if (context == null) {
                        log.warn("No context found for uuid={}, cannot attempt reconnect", uuid);
                        return;
                    }

                    if (!context.canReconnect()) {
                        log.warn("Reconnect limit reached for uuid={}, skipping reconnect", uuid);
                        clearUser(authRequest);
                        return;
                    }

                    if (context.shouldDelayReconnect()) {
                        log.info("Delaying reconnect by 3 seconds for uuid={}", uuid);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    context.increaseReconnect();
                    log.info("increaseReconnect");
                    openConnect(authRequest, true);
                });
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };
    }

    private void startConnectionThread(UserConnectionContext context) {
        String uuid = context.getUuid();

        Thread thread = new Thread(() -> withMDC(uuid, () -> {
            log.info("2. Create thread ws: {}", uuid);
            String url = envConfig.getBaseWsApi() + "/ws/" + context.getListenKey();
            context.setWsClient(new StandardWebSocketClient());
            context.getWsClient().execute(context.getWsHandler(), url);
        }));

        thread.setName("ws-" + uuid);
        thread.setDaemon(true);
        thread.start();
        context.setConnectionThread(thread);
        userContexts.put(uuid, context);
    }

    private void startRenewTask(UserConnectionContext context) {
        String uuid = context.getUuid();
        log.info("4. Create thread renew task: {}", uuid);

        if (context.getRenewTask() != null) {
            log.info("Renew task already running for uuid: {}", uuid);
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "renew-task-" + uuid);
            t.setDaemon(true);
            return t;
        });

        String apiKey = context.getApiKey();

        Runnable renewTask = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = listenKeyApi.update(apiKey);

                    if (success) {
                        log.info("Successfully renewed listenKey for uuid: {}", uuid);
                        context.setCountRetryKey(0);

                        // Schedule lần kế tiếp sau đúng 50 phút
                        ScheduledFuture<?> next = scheduler.schedule(this, RENEW_INTERVAL_MINUTES, TimeUnit.MINUTES);
                        context.setRenewTask(next);

                    } else {
                        handleRetryOrClose();
                    }

                } catch (ResourceAccessException ex) {
                    log.error("Network error renewing listenKey for uuid {}: {}", uuid, ex.getMessage());
                    String apiKeyNew = listenKeyApi.create(apiKey);
                    if (apiKeyNew != null) {
                        context.setListenKey(apiKeyNew);
                        context.setCountRetryKey(0);
                        log.info("Created new listenKey after network error for uuid {}: {}", uuid, apiKeyNew);

                        ScheduledFuture<?> next = scheduler.schedule(this, RENEW_INTERVAL_MINUTES, TimeUnit.MINUTES);
                        context.setRenewTask(next);
                    } else {
                        handleRetryOrClose();
                    }

                } catch (Exception e) {
                    log.error("Error renewing listenKey for uuid {}: {}", uuid, e.getMessage());
                    handleRetryOrClose();
                }
            }

            private void handleRetryOrClose() {
                int retryCount = context.getCountRetryKey() == null ? 0 : context.getCountRetryKey();
                if (retryCount >= 1) {
                    log.warn("Retry already attempted for uuid: {} — closing session", uuid);
                    context.closeSession();
                    return;
                }

                log.warn("Failed to renew listenKey for uuid: {}, will retry once in {}s", uuid, ERROR_DELAY_SECONDS);
                context.setCountRetryKey(retryCount + 1);

                ScheduledFuture<?> retry = scheduler.schedule(this, ERROR_DELAY_SECONDS, TimeUnit.SECONDS);
                context.setRenewTask(retry);
            }
        };

        // Lên lịch lần đầu tiên sau 50 phút
        ScheduledFuture<?> firstTask = scheduler.schedule(renewTask, RENEW_INTERVAL_MINUTES, TimeUnit.MINUTES);
        context.setScheduler(scheduler);
        context.setRenewTask(firstTask);
    }

    /*
    private void startRenewTask(UserConnectionContext context) {
        String uuid = context.getUuid();
        log.info("4. Create thread renew task: {}", uuid);

        if (context.getRenewTask() != null) {
            log.info("Renew task already running for uuid: {}", uuid);
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "renew-task-" + uuid);
            t.setDaemon(true);
            return t;
        });

        String apiKey = context.getApiKey();
        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean success = listenKeyApi.update(apiKey);
                if (!success) {
                    log.warn("Failed to renew listenKey for uuid: {}", uuid);
                    context.closeSession();
                } else {
                    log.info("Successfully renewed listenKey for uuid: {}", uuid);
                }
            } catch (ResourceAccessException exception) {
                log.error("Network error renewing listenKey for uuid {}: {}", uuid, exception.getMessage());
                String apiKeyNew = listenKeyApi.create(apiKey);
                if (apiKeyNew != null) {
                    context.setListenKey(apiKeyNew);
                    log.info("Created new listenKey after network error for uuid {}: {}", uuid, apiKeyNew);
                } else {
                    log.error("Failed to create new listenKey after network error for uuid {}", uuid);
                    context.closeSession();
                }
            } catch (Exception e) {
                log.error("Error renewing listenKey for uuid {}: {}", uuid, e.getMessage());
                context.closeSession();
            }
        }, RENEW_INTERVAL_MINUTES, RENEW_INTERVAL_MINUTES, TimeUnit.MINUTES);

        context.setScheduler(scheduler);
        context.setRenewTask(task);
    }
    */

    private void clearUser(AuthRequest authRequest) {
        String uuid = authRequest.getUuid();
        sendTelegramService.sendDisconnectMessage(authRequest);
        userService.updateStatusConnectAsync(List.of(uuid), ConnectedEnum.DISCONNECTED.name());
        UserConnectionContext context = userContexts.remove(uuid);
        if (context != null) {
            context.cleanup();
            log.info("Cleaned up WebSocket resources for uuid: {}", uuid);
        } else {
            log.warn("No active WebSocket context to clean for uuid: {}", uuid);
        }
    }
}
