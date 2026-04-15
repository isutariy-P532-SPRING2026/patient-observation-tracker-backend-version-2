package com.patienttracker.resourceaccess;

import com.patienttracker.domain.PhenomenonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PhenomenonTypeRepository extends JpaRepository<PhenomenonType, Long> {

    Optional<PhenomenonType> findByName(String name);

}