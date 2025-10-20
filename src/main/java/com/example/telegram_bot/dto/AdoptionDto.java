package com.example.telegram_bot.dto;

import com.example.telegram_bot.jpa.TrialStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdoptionDto {
    private Long id;
    private Long userId;
    private Long animalId;
    private Long shelterId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate adoptionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate trialEndDate;

    private TrialStatus trialStatus;
}