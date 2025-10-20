package com.example.telegram_bot.dto;

import com.example.telegram_bot.jpa.TrialStatus;
import lombok.Data;

@Data
public class UpdateTrialStatusRequest {
    private TrialStatus trialStatus;
    private Integer extendDays;
}