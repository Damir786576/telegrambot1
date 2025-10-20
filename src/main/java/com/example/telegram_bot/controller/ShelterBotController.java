package com.example.telegram_bot.controller;

import com.example.telegram_bot.dto.*;
import com.example.telegram_bot.service.AdoptionService;
import com.example.telegram_bot.service.AnimalService;
import com.example.telegram_bot.service.ReportService;
import com.example.telegram_bot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Shelter Bot API", description = "REST API для управления приютом")
@CrossOrigin(origins = "*")
public class ShelterBotController {

    private final UserService userService;
    private final AnimalService animalService;
    private final AdoptionService adoptionService;
    private final ReportService reportService;

    public ShelterBotController(UserService userService, AnimalService animalService,
                                AdoptionService adoptionService, ReportService reportService) {
        this.userService = userService;
        this.animalService = animalService;
        this.adoptionService = adoptionService;
        this.reportService = reportService;
    }

    @GetMapping("/users")
    @Operation(summary = "Получить всех пользователей")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersDto());
    }

    @GetMapping("/users/{chatId}")
    @Operation(summary = "Получить пользователя по chatId")
    public ResponseEntity<UserDto> getUserByChatId(@PathVariable Long chatId) {
        UserDto user = userService.getUserByChatIdDto(chatId);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @GetMapping("/animals")
    @Operation(summary = "Получить всех доступных животных")
    public ResponseEntity<List<AnimalDto>> getAvailableAnimals() {
        return ResponseEntity.ok(animalService.getAvailableAnimalsDto());
    }

    @GetMapping("/animals/{id}")
    @Operation(summary = "Получить животное по ID")
    public ResponseEntity<AnimalDto> getAnimalById(@PathVariable Long id) {
        AnimalDto animal = animalService.getAnimalByIdDto(id);
        return animal != null ? ResponseEntity.ok(animal) : ResponseEntity.notFound().build();
    }

    @PostMapping("/animals")
    @Operation(summary = "Добавить новое животное")
    public ResponseEntity<AnimalDto> createAnimal(@RequestBody CreateAnimalRequest request) {
        AnimalDto animal = animalService.createAnimal(request);
        return ResponseEntity.ok(animal);
    }

    @GetMapping("/adoptions")
    @Operation(summary = "Получить все усыновления")
    public ResponseEntity<List<AdoptionDto>> getAllAdoptions() {
        return ResponseEntity.ok(adoptionService.getAllAdoptionsDto());
    }

    @GetMapping("/adoptions/user/{chatId}")
    @Operation(summary = "Получить усыновления пользователя")
    public ResponseEntity<List<AdoptionDto>> getUserAdoptions(@PathVariable Long chatId) {
        return ResponseEntity.ok(adoptionService.getUserAdoptionsDto(chatId));
    }

    @PostMapping("/adoptions")
    @Operation(summary = "Создать усыновление")
    public ResponseEntity<AdoptionDto> createAdoption(@RequestBody AdoptionDto request) {
        AdoptionDto adoption = adoptionService.createAdoption(request);
        return ResponseEntity.ok(adoption);
    }

    @PutMapping("/adoptions/{id}/trial-status")
    @Operation(summary = "Обновить статус испытательного срока")
    public ResponseEntity<AdoptionDto> updateTrialStatus(@PathVariable Long id,
                                                         @RequestBody UpdateTrialStatusRequest request) {
        AdoptionDto adoption = adoptionService.updateTrialStatus(id, request);
        return adoption != null ? ResponseEntity.ok(adoption) : ResponseEntity.notFound().build();
    }

    @GetMapping("/reports/adoption/{adoptionId}")
    @Operation(summary = "Получить все отчеты по усыновлению")
    public ResponseEntity<List<ReportDto>> getReportsByAdoption(@PathVariable Long adoptionId) {
        return ResponseEntity.ok(reportService.getReportsByAdoptionDto(adoptionId));
    }

    @PostMapping("/reports")
    @Operation(summary = "Создать отчет")
    public ResponseEntity<ReportDto> createReport(@RequestBody ReportDto request) {
        ReportDto report = reportService.createReport(request);
        return ResponseEntity.ok(report);
    }
}