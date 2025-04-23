package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.repository.AdoptionRepository;
import org.springframework.stereotype.Service;

@Service
public class AdoptionService {
    private final AdoptionRepository repo;

    public AdoptionService(AdoptionRepository repo) {
        this.repo = repo;
    }

    public void save(AdoptionEntity entity) {
        repo.save(entity);
    }

    public AdoptionEntity findById(Long id) {
        return repo.findById(id).orElse(null);
    }
}