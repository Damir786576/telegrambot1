package com.example.telegram_bot.repository;

import com.example.telegram_bot.jpa.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {
    boolean existsByChatId(Long chatId);
    UserEntity findByChatId(Long chatId);
}
