package com.example.telegram_bot.test;

import com.example.telegram_bot.jpa.AdoptionEntity;
import com.example.telegram_bot.jpa.AnimalEntity;
import com.example.telegram_bot.jpa.TrialStatus;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.repository.AdoptionRepository;
import com.example.telegram_bot.service.AdoptionService;
import com.example.telegram_bot.service.AnimalService;
import com.example.telegram_bot.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdoptionServiceTest {

    @Mock
    private AdoptionRepository adoptionRepository;

    @Mock
    private AnimalService animalService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdoptionService adoptionService;

    private UserEntity user;
    private AnimalEntity animal;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setChatId(123L);

        animal = new AnimalEntity();
        animal.setId(1L);
        animal.setName("Fluffy");
        animal.setType("cat");
        animal.setAge(2);
    }

    @Test
    void testSaveAdoption() {
        AdoptionEntity adoption = new AdoptionEntity();
        adoption.setUser(user);
        adoption.setAnimal(animal);
        adoption.setAdoptionDate(LocalDate.now());
        adoption.setTrialStatus(TrialStatus.IN_PROGRESS);

        when(adoptionRepository.save(any(AdoptionEntity.class))).thenReturn(adoption);

        adoptionService.save(adoption);

        verify(adoptionRepository).save(adoption);
    }

    @Test
    void testFindById() {
        AdoptionEntity adoption = new AdoptionEntity();
        adoption.setId(1L);

        when(adoptionRepository.findById(1L)).thenReturn(Optional.of(adoption));

        AdoptionEntity found = adoptionService.findById(1L);
        assertEquals(1L, found.getId());
    }
}
