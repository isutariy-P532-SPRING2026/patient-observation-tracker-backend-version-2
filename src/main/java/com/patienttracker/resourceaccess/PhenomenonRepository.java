package com.patienttracker.resourceaccess;

import com.patienttracker.domain.Phenomenon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhenomenonRepository extends JpaRepository<Phenomenon, Long> {
    List<Phenomenon> findByPhenomenonTypeId(Long phenomenonTypeId);
}