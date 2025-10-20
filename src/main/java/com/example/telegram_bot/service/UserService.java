package com.example.telegram_bot.service;

import com.example.telegram_bot.dto.UserDto;
import com.example.telegram_bot.jpa.Role;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
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
        return user != null && user.getRole() == Role.ROLE_ADMIN;
    }

    public void makeAdmin(Long chatId) {
        UserEntity user = findByChatId(chatId);
        if (user != null) {
            user.setRole(Role.ROLE_ADMIN);
            save(user);
        }
    }

    public List<UserDto> getAllUsersDto() {
        return findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDto getUserByChatIdDto(Long chatId) {
        UserEntity user = findByChatId(chatId);
        return user != null ? toDto(user) : null;
    }

    private UserDto toDto(UserEntity user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setChatId(user.getChatId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setSubscribed(user.isSubscribed());
        dto.setRole(user.getRole().name());
        return dto;
    }

    public List<UserEntity> findAll() {
        List<UserEntity> users = (List<UserEntity>) repo.findAll();
        log.info("Получен список всех пользователей, размер: {}", users.size());
        return users;
    }
}