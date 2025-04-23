package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.repository.AnimalRepository;
import org.springframework.stereotype.Service;

@Service
public class AnimalService {
    private final AnimalRepository repo;

    public AnimalService(AnimalRepository repo) {
        this.repo = repo;
    }

    public void save(AnimalEntity entity) {
        repo.save(entity);
    }

    public AnimalEntity findById(Long id) {
        return repo.findById(id).orElse(null);
    }
    public Iterable<AnimalEntity> findAll() {
        return repo.findAll();
    }
}