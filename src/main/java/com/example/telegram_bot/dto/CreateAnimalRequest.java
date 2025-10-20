package com.example.telegram_bot.dto;

import lombok.Data;

@Data
public class CreateAnimalRequest {
    private String name;
    private String type;
    private Integer age;
}