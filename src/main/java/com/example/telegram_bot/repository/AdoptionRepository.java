package com.example.telegram_bot.repository;

import com.example.telegram_bot.jpa.AdoptionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdoptionRepository extends CrudRepository<AdoptionEntity, Long> {
}