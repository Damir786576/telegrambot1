package com.example.telegram_bot.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class ReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "adoption_id", nullable = false)
    private AdoptionEntity adoption;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "content")
    private String content;
}