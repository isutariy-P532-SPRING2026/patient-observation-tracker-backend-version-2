package com.patienttracker.config;

import com.patienttracker.domain.AssociativeFunction;
import com.patienttracker.domain.Phenomenon;
import com.patienttracker.domain.PhenomenonType;
import com.patienttracker.domain.Protocol;
import com.patienttracker.domain.enums.AccuracyRating;
import com.patienttracker.domain.enums.MeasurementKind;
import com.patienttracker.resourceaccess.AssociativeFunctionRepository;
import com.patienttracker.resourceaccess.PhenomenonRepository;
import com.patienttracker.resourceaccess.PhenomenonTypeRepository;
import com.patienttracker.resourceaccess.ProtocolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PhenomenonTypeRepository ptRepo;
    private final PhenomenonRepository phenRepo;
    private final ProtocolRepository protocolRepo;
    private final AssociativeFunctionRepository afRepo;

    public DataInitializer(PhenomenonTypeRepository ptRepo,
            PhenomenonRepository phenRepo,
            ProtocolRepository protocolRepo,
            AssociativeFunctionRepository afRepo) {
        this.ptRepo = ptRepo;
        this.phenRepo = phenRepo;
        this.protocolRepo = protocolRepo;
        this.afRepo = afRepo;
    }

    @Override
    public void run(String... args) {
        if (ptRepo.count() == 0)
            seedPhenomenonTypes();
        if (protocolRepo.count() == 0)
            seedProtocols();
        if (afRepo.count() == 0)
            seedRules();
    }

    private void seedPhenomenonTypes() {
        createQuant("Body Temperature", Set.of("Celsius", "Fahrenheit", "Kelvin"));
        createQuant("Blood Glucose", Set.of("mg/dL", "mmol/L"));
        createQuant("Systolic Blood Pressure", Set.of("mmHg"));
        createQuant("Diastolic Blood Pressure", Set.of("mmHg"));
        createQuant("Heart Rate", Set.of("bpm"));
        createQuant("Body Weight", Set.of("kg", "lbs"));
        createQuant("Body Height", Set.of("cm", "inches"));
        createQuant("Oxygen Saturation", Set.of("%"));
        createQuant("Respiratory Rate", Set.of("breaths/min"));

        createQual("Blood Group",
                List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        createQual("Structural Condition",
                List.of("Excellent", "Good", "Fair", "Poor", "Critical"));
        createQual("Pain Level",
                List.of("None", "Mild", "Moderate", "Severe", "Extreme"));
        createQual("Level of Consciousness",
                List.of("Alert", "Verbal", "Pain", "Unresponsive"));
        createQual("Mobility Status",
                List.of("Independent", "Assisted", "Dependent", "Bedbound"));
    }

    private void createQuant(String name, Set<String> units) {
        PhenomenonType pt = new PhenomenonType();
        pt.setName(name);
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(units);
        ptRepo.save(pt);
    }

    private void createQual(String name, List<String> phenomena) {
        PhenomenonType pt = new PhenomenonType();
        pt.setName(name);
        pt.setKind(MeasurementKind.QUALITATIVE);
        ptRepo.save(pt);
        for (String pName : phenomena) {
            Phenomenon p = new Phenomenon();
            p.setName(pName);
            p.setPhenomenonType(pt);
            phenRepo.save(p);
        }
    }

    private void seedRules() {
        PhenomenonType bodyTemp = ptRepo.findByName("Body Temperature").orElse(null);
        PhenomenonType heartRate = ptRepo.findByName("Heart Rate").orElse(null);
        PhenomenonType glucose = ptRepo.findByName("Blood Glucose").orElse(null);
        PhenomenonType weight = ptRepo.findByName("Body Weight").orElse(null);
        PhenomenonType systolicBP = ptRepo.findByName("Systolic Blood Pressure").orElse(null);
        PhenomenonType oxygenSat = ptRepo.findByName("Oxygen Saturation").orElse(null);

        if (bodyTemp != null && heartRate != null && systolicBP != null) {
            AssociativeFunction rule1 = new AssociativeFunction();
            rule1.setName("Systemic Inflammatory Response");
            rule1.setArgumentConcepts(Set.of(bodyTemp, heartRate)); // SET not List
            rule1.setProductConcept(systolicBP);
            rule1.setActive(true);
            afRepo.save(rule1);
        }

        if (glucose != null && weight != null && systolicBP != null) {
            AssociativeFunction rule2 = new AssociativeFunction();
            rule2.setName("Metabolic Risk");
            rule2.setArgumentConcepts(Set.of(glucose, weight)); // SET not List
            rule2.setProductConcept(systolicBP);
            rule2.setActive(true);
            afRepo.save(rule2);
        }

        if (oxygenSat != null && heartRate != null && bodyTemp != null) {
            AssociativeFunction rule3 = new AssociativeFunction();
            rule3.setName("Respiratory Compromise");
            rule3.setArgumentConcepts(Set.of(oxygenSat, heartRate)); // SET not List
            rule3.setProductConcept(bodyTemp);
            rule3.setActive(true);
            afRepo.save(rule3);
        }
    }

    private void seedProtocols() {
        Object[][] data = {
                { "Standard Blood Pressure Protocol", "Two readings 5 min apart, patient seated.",
                        AccuracyRating.HIGH },
                { "Fasting Blood Glucose Protocol", "Patient fasts 8 hrs prior to measurement.", AccuracyRating.HIGH },
                { "Oral Temperature Protocol", "Thermometer under tongue for 3 minutes.", AccuracyRating.MEDIUM },
                { "Pulse Oximetry Protocol", "Probe on index finger, patient at rest.", AccuracyRating.MEDIUM },
                { "Manual Heart Rate Protocol", "Radial pulse counted for 60 seconds.", AccuracyRating.MEDIUM },
                { "Random Blood Glucose Protocol", "Sample taken regardless of meal timing.", AccuracyRating.MEDIUM },
                { "Body Weight Protocol", "Patient weighed in light clothing, no shoes.", AccuracyRating.HIGH },
                { "Tympanic Temperature Protocol", "Ear thermometer, right ear preferred.", AccuracyRating.LOW },
                { "Visual Pain Assessment Protocol", "Patient self-reports on numeric 0-10 scale.",
                        AccuracyRating.LOW },
                { "Standard Observation Protocol", "General clinical observation by trained staff.",
                        AccuracyRating.MEDIUM },
        };
        for (Object[] row : data) {
            Protocol p = new Protocol();
            p.setName((String) row[0]);
            p.setDescription((String) row[1]);
            p.setAccuracyRating((AccuracyRating) row[2]);
            protocolRepo.save(p);
        }
    }
}