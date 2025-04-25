package com.example.telegram_bot.repository;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.ReportEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends CrudRepository<ReportEntity, Long> {
    List<ReportEntity> findByAdoptionOrderByReportDateDesc(AdoptionEntity adoption);
}