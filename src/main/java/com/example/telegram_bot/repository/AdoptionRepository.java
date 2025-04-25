package com.example.telegram_bot.repository;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.jpa.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdoptionRepository extends CrudRepository<AdoptionEntity, Long> {
    AdoptionEntity findByAnimal(AnimalEntity animal);
    List<AdoptionEntity> findByUser(UserEntity user);
    List<AdoptionEntity> findAll();
}