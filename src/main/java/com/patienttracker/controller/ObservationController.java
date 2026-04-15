package com.patienttracker.controller;

import com.patienttracker.domain.Observation;
import com.patienttracker.domain.enums.Presence;
import com.patienttracker.manager.ObservationManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/observations")
public class ObservationController {

    private final ObservationManager observationManager;

    public ObservationController(ObservationManager observationManager) {
        this.observationManager = observationManager;
    }

    @PostMapping("/measurement")
    public ResponseEntity<Observation> recordMeasurement(@RequestBody Map<String, Object> body) {
        Long patientId        = toLong(body.get("patientId"));
        Long phenomenonTypeId = toLong(body.get("phenomenonTypeId"));
        Double amount         = toDouble(body.get("amount"));
        String unit           = (String) body.get("unit");
        LocalDateTime appTime = body.get("applicabilityTime") != null
            ? LocalDateTime.parse((String) body.get("applicabilityTime")) : null;
        Long protocolId       = body.get("protocolId") != null ? toLong(body.get("protocolId")) : null;

        return ResponseEntity.ok(
            observationManager.recordMeasurement(patientId, phenomenonTypeId, amount, unit, appTime, protocolId));
    }

    @PostMapping("/category")
    public ResponseEntity<Observation> recordCategory(@RequestBody Map<String, Object> body) {
        Long patientId     = toLong(body.get("patientId"));
        Long phenomenonId  = toLong(body.get("phenomenonId"));
        Presence presence  = Presence.valueOf((String) body.get("presence"));
        LocalDateTime appTime = body.get("applicabilityTime") != null
            ? LocalDateTime.parse((String) body.get("applicabilityTime")) : null;
        Long protocolId    = body.get("protocolId") != null ? toLong(body.get("protocolId")) : null;

        return ResponseEntity.ok(
            observationManager.recordCategoryObservation(patientId, phenomenonId, presence, appTime, protocolId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Observation> rejectObservation(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(observationManager.rejectObservation(id, body.get("reason")));
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
    private Double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }
}