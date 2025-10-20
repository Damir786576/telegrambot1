package com.example.telegram_bot.test;

import com.example.telegram_bot.dto.AnimalDto;
import com.example.telegram_bot.jpa.*;
import com.example.telegram_bot.repository.AdoptionRepository;
import com.example.telegram_bot.repository.AnimalRepository;
import com.example.telegram_bot.service.AnimalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock private AnimalRepository animalRepository;
    @Mock private AdoptionRepository adoptionRepository;

    @InjectMocks private AnimalService animalService;

    private AnimalEntity animal;

    @BeforeEach
    void setUp() {
        animal = new AnimalEntity();
        animal.setId(1L);
        animal.setName("Fluffy");
        animal.setType("cat");
        animal.setAge(2);
    }

    @Test
    void testSaveAnimal() {
        when(animalRepository.save(any(AnimalEntity.class))).thenReturn(animal);
        animalService.save(animal);
        verify(animalRepository).save(animal);
    }

    @Test
    void testFindById_Found() {
        when(animalRepository.findById(1L)).thenReturn(java.util.Optional.of(animal));
        AnimalEntity found = animalService.findById(1L);
        assertEquals("Fluffy", found.getName());
    }

    @Test
    void testFindById_NotFound() {
        when(animalRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        AnimalEntity found = animalService.findById(999L);
        assertNull(found);
    }

    @Test
    void testGetAnimalListString_Available() {
        when(animalRepository.findAll()).thenReturn(Arrays.asList(animal));
        when(adoptionRepository.findByAnimal(animal)).thenReturn(null);

        String result = animalService.getAnimalListString();
        assertTrue(result.contains("Fluffy"));
        assertTrue(result.contains("ID: 1"));
    }

    @Test
    void testGetAvailableAnimalsDto() {
        when(animalRepository.findAll()).thenReturn(Arrays.asList(animal));
        when(adoptionRepository.findByAnimal(animal)).thenReturn(null);

        List<AnimalDto> result = animalService.getAvailableAnimalsDto();
        assertEquals(1, result.size());
        assertEquals("Fluffy", result.get(0).getName());
    }
}
