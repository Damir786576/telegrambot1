package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.ShelterEntity;
import com.example.telegram_bot.repository.ShelterRepository;
import org.springframework.stereotype.Service;

@Service
public class ShelterService {
    private final ShelterRepository repo;

    public ShelterService(ShelterRepository repo) {
        this.repo = repo;
    }

    public void save(ShelterEntity entity) {
        repo.save(entity);
    }

    public ShelterEntity findById(Long id) {
        return repo.findById(id).orElse(null);
    }
}