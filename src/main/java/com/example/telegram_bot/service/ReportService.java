package com.example.telegram_bot.service;

import com.example.telegram_bot.dto.ReportDto;
import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.ReportEntity;
import com.example.telegram_bot.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public List<ReportDto> getReportsByAdoptionDto(Long adoptionId) {
        AdoptionEntity adoption = new AdoptionEntity();
        adoption.setId(adoptionId);
        List<ReportEntity> reports = reportRepository.findByAdoptionOrderByReportDateDesc(adoption);
        return reports.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReportDto createReport(ReportDto request) {
        ReportEntity entity = new ReportEntity();
        entity.setAdoption(new AdoptionEntity());
        entity.getAdoption().setId(request.getAdoptionId());
        entity.setContent(request.getContent());
        entity.setPhotoUrl(request.getPhotoUrl());
        entity.setReportType(request.getReportType());
        entity.setReportDate(request.getReportDate());

        save(entity);
        return toDto(entity);
    }

    public ReportDto toDto(ReportEntity entity) {
        ReportDto dto = new ReportDto();
        dto.setId(entity.getId());
        dto.setAdoptionId(entity.getAdoption().getId());
        dto.setReportDate(entity.getReportDate());
        dto.setContent(entity.getContent());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setReportType(entity.getReportType());
        return dto;
    }

    public ReportEntity toEntity(ReportDto dto) {
        ReportEntity entity = new ReportEntity();
        entity.setId(dto.getId());
        entity.setContent(dto.getContent());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setReportType(dto.getReportType());
        entity.setReportDate(dto.getReportDate());
        return entity;
    }

    public List<ReportDto> getAllReportsDto() {
        List<ReportEntity> reports = new ArrayList<>();
        reportRepository.findAll().forEach(reports::add);
        return reports.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ReportDto getReportByIdDto(Long id) {
        return reportRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }
}
