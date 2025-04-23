package com.example.telegram_bot.repository;

import com.example.telegram_bot.jpa.ShelterEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelterRepository extends CrudRepository<ShelterEntity, Long> {
}
