package com.example.telegram_bot.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private Long chatId;
    private String name;
    private String phone;
    private boolean subscribed;
    private String role;
}