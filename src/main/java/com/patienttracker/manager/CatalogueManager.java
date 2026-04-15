package com.patienttracker.manager;

import com.patienttracker.domain.Phenomenon;
import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.domain.Protocol;
import com.patienttracker.resourceaccess.PhenomenonRepository;
import com.patienttracker.resourceaccess.PhenomenonTypeRepository;
import com.patienttracker.resourceaccess.ProtocolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogueManager {

    private final PhenomenonTypeRepository phenomenonTypeRepository;
    private final PhenomenonRepository phenomenonRepository;
    private final ProtocolRepository protocolRepository;

    public CatalogueManager(PhenomenonTypeRepository phenomenonTypeRepository,
                             PhenomenonRepository phenomenonRepository,
                             ProtocolRepository protocolRepository) {
        this.phenomenonTypeRepository = phenomenonTypeRepository;
        this.phenomenonRepository = phenomenonRepository;
        this.protocolRepository = protocolRepository;
    }

    // ---------- Phenomenon Types ----------
    public List<PhenomenonType> getAllPhenomenonTypes() {
        return phenomenonTypeRepository.findAll();
    }

    public PhenomenonType createPhenomenonType(PhenomenonType pt) {
        return phenomenonTypeRepository.save(pt);
    }

    public PhenomenonType getPhenomenonType(Long id) {
        return phenomenonTypeRepository.findById(id).orElseThrow();
    }

    // ---------- Phenomena ----------
    public Phenomenon createPhenomenon(Long phenomenonTypeId, Phenomenon phenomenon) {
        PhenomenonType pt = phenomenonTypeRepository.findById(phenomenonTypeId).orElseThrow();
        phenomenon.setPhenomenonType(pt);
        return phenomenonRepository.save(phenomenon);
    }

    public List<Phenomenon> getPhenomenaForType(Long phenomenonTypeId) {
        return phenomenonRepository.findByPhenomenonTypeId(phenomenonTypeId);
    }

    // ---------- Protocols ----------
    public List<Protocol> getAllProtocols() {
        return protocolRepository.findAll();
    }

    public Protocol createProtocol(Protocol protocol) {
        return protocolRepository.save(protocol);
    }
}