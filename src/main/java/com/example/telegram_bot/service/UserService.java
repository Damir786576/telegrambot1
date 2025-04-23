package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public void save(UserEntity entity) {
        repo.save(entity);
    }

    public boolean existsByChatId(Long chatId) {
        return repo.existsByChatId(chatId);
    }

    public UserEntity findByChatId(Long chatId) {
        return repo.findByChatId(chatId);
    }
}
