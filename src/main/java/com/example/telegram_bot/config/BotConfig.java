package com.example.telegram_bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Конфигурация Telegram-бота.
 */
@Configuration
@PropertySource("application.properties")
public class BotConfig {

    /**
     * Имя бота.
     */
    @Value("${bot.name}")
    private String botName;
    /**
     * Токен бота.
     */
    @Value("${bot.token}")
    private String token;

    /**
     * Возвращает имя бота.
     */
    public String getBotName() {
        return botName;
    }

    /**
     * Возвращает токен бота.
     */
    public String getToken() {
        return token;
    }
}