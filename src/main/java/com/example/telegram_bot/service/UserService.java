package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.Role;
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

    public boolean isAdmin(Long chatId) {
        UserEntity user = findByChatId(chatId);
        return user!= null && user.getRole() == Role.ROLE_ADMIN;
    }

    public void makeAdmin(Long chatId) {
        UserEntity user = findByChatId(chatId);
        if (user != null) {
            user.setRole(Role.ROLE_ADMIN);
            save(user);
        }
    }
}
