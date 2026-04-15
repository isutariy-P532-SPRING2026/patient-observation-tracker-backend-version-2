package com.patienttracker.controller;

import com.patienttracker.domain.Phenomenon;
import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.domain.Protocol;
import com.patienttracker.manager.CatalogueManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.patienttracker.domain.AssociativeFunction;
import com.patienttracker.resourceaccess.AssociativeFunctionRepository;

import java.util.List;

@RestController
public class CatalogueController {

    private final CatalogueManager catalogueManager;
    private final AssociativeFunctionRepository afRepo;

    public CatalogueController(CatalogueManager catalogueManager,
            AssociativeFunctionRepository afRepo) {
        this.catalogueManager = catalogueManager;
        this.afRepo = afRepo;
    }

    // ---- Phenomenon Types ----

    @GetMapping("/api/phenomenon-types")
    public List<PhenomenonType> getAllPhenomenonTypes() {
        return catalogueManager.getAllPhenomenonTypes();
    }

    @PostMapping("/api/phenomenon-types")
    public ResponseEntity<PhenomenonType> createPhenomenonType(@RequestBody PhenomenonType pt) {
        return ResponseEntity.ok(catalogueManager.createPhenomenonType(pt));
    }

    @GetMapping("/api/phenomenon-types/{id}")
    public ResponseEntity<PhenomenonType> getPhenomenonType(@PathVariable Long id) {
        return ResponseEntity.ok(catalogueManager.getPhenomenonType(id));
    }

    // ---- Phenomena (within a type) ----

    @PostMapping("/api/phenomenon-types/{id}/phenomena")
    public ResponseEntity<Phenomenon> createPhenomenon(@PathVariable Long id,
            @RequestBody Phenomenon phenomenon) {
        return ResponseEntity.ok(catalogueManager.createPhenomenon(id, phenomenon));
    }

    @GetMapping("/api/phenomenon-types/{id}/phenomena")
    public List<Phenomenon> getPhenomena(@PathVariable Long id) {
        return catalogueManager.getPhenomenaForType(id);
    }

    // ---- Protocols ----

    @GetMapping("/api/protocols")
    public List<Protocol> getAllProtocols() {
        return catalogueManager.getAllProtocols();
    }

    @PostMapping("/api/protocols")
    public ResponseEntity<Protocol> createProtocol(@RequestBody Protocol protocol) {
        return ResponseEntity.ok(catalogueManager.createProtocol(protocol));
    }

    @GetMapping("/api/rules")
    public List<AssociativeFunction> getRules() {
        return afRepo.findAll();
    }

    @PostMapping("/api/rules")
    public ResponseEntity<AssociativeFunction> createRule(@RequestBody AssociativeFunction rule) {
        return ResponseEntity.ok(afRepo.save(rule));
    }
}