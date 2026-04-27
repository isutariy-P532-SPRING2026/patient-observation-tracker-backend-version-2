package com.patienttracker.resourceaccess;

import com.patienttracker.domain.ArgumentWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArgumentWeightRepository extends JpaRepository<ArgumentWeight, Long> {}