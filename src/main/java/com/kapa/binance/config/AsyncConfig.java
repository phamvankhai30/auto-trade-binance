package com.kapa.binance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "copierExecutor")
    public Executor copierExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // Số thread tối thiểu
        executor.setMaxPoolSize(50);       // Số thread tối đa
        executor.setQueueCapacity(200);    // Hàng đợi task
        executor.setThreadNamePrefix("copier-");
        executor.initialize();
        return executor;
    }
}
