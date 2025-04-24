package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.repository.AnimalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnimalService {
    private static final Logger log = LoggerFactory.getLogger(AnimalService.class);
    private final AnimalRepository repository;

    public AnimalService(AnimalRepository repository) {
        this.repository = repository;
    }

    public void save(AnimalEntity animal) {
        try {
            repository.save(animal);
            log.info("Сохранено животное: name={}, type={}, age={}", animal.getName(), animal.getType(), animal.getAge());
        } catch (Exception e) {
            log.error("Ошибка сохранения животного: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Iterable<AnimalEntity> findAll() {
        try {
            Iterable<AnimalEntity> animals = repository.findAll();
            log.info("Получен список всех животных");
            return animals;
        } catch (Exception e) {
            log.error("Ошибка получения списка животных: {}", e.getMessage(), e);
            throw e;
        }
    }
}