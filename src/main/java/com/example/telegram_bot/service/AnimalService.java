package com.example.telegram_bot.service;

import com.example.telegram_bot.dto.AnimalDto;
import com.example.telegram_bot.dto.CreateAnimalRequest;
import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.TrialStatus;
import com.example.telegram_bot.repository.AdoptionRepository;
import com.example.telegram_bot.repository.AnimalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnimalService {
    private static final Logger log = LoggerFactory.getLogger(AnimalService.class);
    private final AnimalRepository repo;
    private final AdoptionRepository adoptionRepository;

    public AnimalService(AnimalRepository repo, AdoptionRepository adoptionRepository) {
        this.repo = repo;
        this.adoptionRepository = adoptionRepository;
    }

    public void save(AnimalEntity animal) {
        repo.save(animal);
        log.info("Сохранено животное ID={}", animal.getId());
    }

    public void delete(AnimalEntity animal) {
        repo.delete(animal);
        log.info("Удалено животное ID={}", animal.getId());
    }

    public AnimalEntity findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public List<AnimalEntity> findAll() {
        List<AnimalEntity> animals = (List<AnimalEntity>) repo.findAll();
        log.info("Найдено {} животных", animals.size());
        return animals;
    }

    public String getAnimalListString() {
        List<AnimalEntity> animals = findAll().stream()
                .filter(animal -> {
                    AdoptionEntity adoption = adoptionRepository.findByAnimal(animal);
                    return adoption == null || adoption.getTrialStatus() == TrialStatus.FAILED;
                })
                .collect(Collectors.toList());

        if (animals.isEmpty()) {
            log.info("Список доступных животных пуст");
            return "🐾 Список животных пуст.";
        }

        StringBuilder response = new StringBuilder("🐾 Список животных в приюте:\n\n");
        for (int i = 0; i < animals.size(); i++) {
            AnimalEntity animal = animals.get(i);
            response.append(String.format("%d. %s %s (ID: %d)\n", i + 1, animal.getType(), animal.getName(), animal.getId()));
            response.append(String.format("   Возраст: %d года\n", animal.getAge()));
            if (i < animals.size() - 1) {
                response.append("\n");
            }
        }
        log.info("Сформирован список доступных животных, размер: {}", animals.size());
        return response.toString();
    }
    public List<AnimalDto> getAvailableAnimalsDto() {
        return getAnimalListString().isEmpty() ?
                List.of() : findAll().stream()
                .filter(this::isAvailable)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public AnimalDto getAnimalByIdDto(Long id) {
        AnimalEntity animal = findById(id);
        return animal != null ? toDto(animal) : null;
    }

    public AnimalDto createAnimal(CreateAnimalRequest request) {
        AnimalEntity animal = new AnimalEntity();
        animal.setName(request.getName());
        animal.setType(request.getType());
        animal.setAge(request.getAge());
        save(animal);
        return toDto(animal);
    }

    private boolean isAvailable(AnimalEntity animal) {
        AdoptionEntity adoption = adoptionRepository.findByAnimal(animal);
        return adoption == null || adoption.getTrialStatus() == TrialStatus.FAILED;
    }

    private AnimalDto toDto(AnimalEntity animal) {
        AnimalDto dto = new AnimalDto();
        dto.setId(animal.getId());
        dto.setName(animal.getName());
        dto.setType(animal.getType());
        dto.setAge(animal.getAge());
        dto.setCreatedAt(animal.getCreatedAt());
        return dto;
    }
}
