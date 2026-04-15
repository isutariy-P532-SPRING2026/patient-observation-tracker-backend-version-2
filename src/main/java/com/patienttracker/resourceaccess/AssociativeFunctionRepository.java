package com.patienttracker.resourceaccess;

import com.patienttracker.domain.AssociativeFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociativeFunctionRepository extends JpaRepository<AssociativeFunction, Long> {
    List<AssociativeFunction> findByActiveTrue();
}