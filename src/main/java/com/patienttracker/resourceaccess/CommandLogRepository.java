package com.patienttracker.resourceaccess;

import com.patienttracker.domain.CommandLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandLogRepository extends JpaRepository<CommandLogEntry, Long> {}