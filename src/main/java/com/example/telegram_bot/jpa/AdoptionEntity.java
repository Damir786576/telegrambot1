package com.example.telegram_bot.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Table(name = "adoptions")
public class AdoptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "animal_id", nullable = false)
    private AnimalEntity animal;

    @Column(name = "adoption_date", nullable = false)
    private LocalDate adoptionDate;
}
