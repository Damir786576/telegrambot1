package com.example.telegram_bot.service;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.ReportEntity;
import com.example.telegram_bot.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public void save(ReportEntity report) {
        reportRepository.save(report);
        log.info("Сохранён отчёт ID={} для усыновления ID={}", report.getId(), report.getAdoption().getId());
    }

    public void deleteByAdoption(AdoptionEntity adoption) {
        try {
            List<ReportEntity> reports = reportRepository.findByAdoptionOrderByReportDateDesc(adoption);
            if (!reports.isEmpty()) {
                reportRepository.deleteAll(reports);
                log.info("Удалено {} отчётов для усыновления ID={}", reports.size(), adoption.getId());
            } else {
                log.info("Отчёты для усыновления ID={} отсутствуют", adoption.getId());
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении отчётов для усыновления ID={}: {}", adoption.getId(), e.getMessage(), e);
            throw e;
        }
    }
}
