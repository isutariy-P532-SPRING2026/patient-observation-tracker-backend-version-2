package com.patienttracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "command_log")
@Getter @Setter @NoArgsConstructor
public class CommandLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String commandType;

    @Column(columnDefinition = "TEXT")
    private String payload;   

    private LocalDateTime executedAt;
    private String user;
}