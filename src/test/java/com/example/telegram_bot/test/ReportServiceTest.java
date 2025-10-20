package com.example.telegram_bot.test;

import com.example.telegram_bot.dto.ReportDto;
import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.ReportEntity;
import com.example.telegram_bot.repository.ReportRepository;
import com.example.telegram_bot.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @InjectMocks
    private ReportService reportService;

    private ReportEntity report;
    private AdoptionEntity adoption;

    @BeforeEach
    void setUp() {
        adoption = new AdoptionEntity();
        adoption.setId(1L);

        report = new ReportEntity();
        report.setId(1L);
        report.setAdoption(adoption);
        report.setContent("Test content");
        report.setReportType("PHOTO");
        report.setReportDate(LocalDateTime.now());
    }

    @Test
    void testSaveReport() {
        reportService.save(report);
        verify(reportRepository).save(report);
    }


    @Test
    void testCreateReport() {
        ReportDto request = new ReportDto();
        request.setAdoptionId(1L);
        request.setContent("New report");
        request.setReportType("WELLBEING");
        request.setReportDate(LocalDateTime.now());

        ReportDto result = reportService.createReport(request);
        assertEquals("New report", result.getContent());
        verify(reportRepository).save(any(ReportEntity.class));
    }

    @Test
    void testToDto() {
        ReportDto dto = reportService.toDto(report);
        assertEquals(1L, dto.getId());
        assertEquals("Test content", dto.getContent());
        assertEquals("PHOTO", dto.getReportType());
    }
}