package com.example.telegram_bot.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "adoptions")
@Getter
@Setter
@NoArgsConstructor
public class AdoptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "animal_id")
    private AnimalEntity animal;

    @Column(name = "adoption_date")
    private LocalDate adoptionDate;

    @Column(name = "shelter_id")
    private Long shelterId;

    @Column(name = "trial_status")
    @Enumerated(EnumType.STRING)
    private TrialStatus trialStatus = TrialStatus.IN_PROGRESS;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;
}