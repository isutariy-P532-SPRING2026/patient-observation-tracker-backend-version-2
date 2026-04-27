package com.patienttracker.config;

import com.patienttracker.domain.*;
import com.patienttracker.domain.enums.*;
import com.patienttracker.resourceaccess.*;
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
    private final ArgumentWeightRepository weightRepo;
    private final AppUserRepository userRepo;

    public DataInitializer(PhenomenonTypeRepository ptRepo,
                           PhenomenonRepository phenRepo,
                           ProtocolRepository protocolRepo,
                           AssociativeFunctionRepository afRepo,
                           ArgumentWeightRepository weightRepo,
                           AppUserRepository userRepo) {
        this.ptRepo = ptRepo;
        this.phenRepo = phenRepo;
        this.protocolRepo = protocolRepo;
        this.afRepo = afRepo;
        this.weightRepo = weightRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void run(String... args) {
        if (ptRepo.count() == 0)       seedPhenomenonTypes();
        if (protocolRepo.count() == 0) seedProtocols();
        seedUsers();               // per-username check inside — safe to call on every startup
        seedRules();               // per-rule name check inside — safe to call on every startup
        seedPainLevelIfMissing();  // re-seeds Pain Level phenomena if the type exists but is empty
    }

    // ---- Users ----

    private void seedUsers() {
        ensureUser("admin", UserRole.ADMIN);
        ensureUser("alice", UserRole.CLINICIAN);
        ensureUser("bob",   UserRole.CLINICIAN);
        ensureUser("staff", UserRole.CLINICIAN);
    }

    private void ensureUser(String username, UserRole role) {
        if (userRepo.findByUsername(username).isEmpty()) {
            AppUser u = new AppUser();
            u.setUsername(username);
            u.setRole(role);
            userRepo.save(u);
        }
    }

    // ---- Phenomenon Types ----

    private void seedPhenomenonTypes() {
        // QUANTITATIVE — with normal ranges for anomaly detection
        createQuant("Body Temperature",          Set.of("Celsius", "Fahrenheit", "Kelvin"), 36.1, 37.2);
        createQuant("Blood Glucose",             Set.of("mg/dL", "mmol/L"),                 70.0, 140.0);
        createQuant("Systolic Blood Pressure",   Set.of("mmHg"),                             90.0, 120.0);
        createQuant("Diastolic Blood Pressure",  Set.of("mmHg"),                             60.0, 80.0);
        createQuant("Heart Rate",                Set.of("bpm"),                              60.0, 100.0);
        createQuant("Body Weight",               Set.of("kg", "lbs"),                        null, null);
        createQuant("Body Height",               Set.of("cm", "inches"),                     null, null);
        createQuant("Oxygen Saturation",         Set.of("%"),                                95.0, 100.0);
        createQuant("Respiratory Rate",          Set.of("breaths/min"),                      12.0, 20.0);

        // QUALITATIVE — with concept hierarchy for Pain Level
        createQual("Blood Group",
                List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        createQual("Structural Condition",
                List.of("Excellent", "Good", "Fair", "Poor", "Critical"));
        seedPainLevelWithHierarchy();
        createQual("Level of Consciousness",
                List.of("Alert", "Verbal", "Pain", "Unresponsive"));
        createQual("Mobility Status",
                List.of("Independent", "Assisted", "Dependent", "Bedbound"));
    }

    private void createQuant(String name, Set<String> units, Double min, Double max) {
        PhenomenonType pt = new PhenomenonType();
        pt.setName(name);
        pt.setKind(MeasurementKind.QUANTITATIVE);
        pt.setAllowedUnits(units);
        if (min != null) pt.setNormalMin(min);
        if (max != null) pt.setNormalMax(max);
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

    /**
     * Seeds Pain Level with a 2-level hierarchy:
     *   Any Pain (root)
     *     ├── Mild
     *     ├── Moderate
     *     └── Severe Pain (mid)
     *           └── Extreme
     * Demonstrates Change 4: parentConcept self-reference + PropagationListener.
     */
    private void seedPainLevelWithHierarchy() {
        PhenomenonType pt = new PhenomenonType();
        pt.setName("Pain Level");
        pt.setKind(MeasurementKind.QUALITATIVE);
        ptRepo.save(pt);

        // Root
        Phenomenon none = phenomenon("None", pt, null);
        Phenomenon anyPain = phenomenon("Any Pain", pt, null);   // root anchor

        // Level 1 children of "Any Pain"
        Phenomenon mild = phenomenon("Mild", pt, anyPain);
        Phenomenon moderate = phenomenon("Moderate", pt, anyPain);
        Phenomenon severePain = phenomenon("Severe Pain", pt, anyPain);

        // Level 2 child of "Severe Pain"
        phenomenon("Extreme", pt, severePain);
    }

    private Phenomenon phenomenon(String name, PhenomenonType pt, Phenomenon parent) {
        Phenomenon p = new Phenomenon();
        p.setName(name);
        p.setPhenomenonType(pt);
        if (parent != null) p.setParentConcept(parent);
        return phenRepo.save(p);
    }

    private void seedPainLevelIfMissing() {
        ptRepo.findByName("Pain Level").ifPresent(pt -> {
            if (phenRepo.findByPhenomenonTypeId(pt.getId()).isEmpty()) {
                Phenomenon none     = phenomenon("None",       pt, null);
                Phenomenon anyPain  = phenomenon("Any Pain",   pt, null);
                Phenomenon mild     = phenomenon("Mild",       pt, anyPain);
                Phenomenon moderate = phenomenon("Moderate",   pt, anyPain);
                Phenomenon severe   = phenomenon("Severe Pain",pt, anyPain);
                phenomenon("Extreme", pt, severe);
            }
        });
    }

    // ---- Rules ----

    private void seedRules() {
        PhenomenonType bodyTemp   = ptRepo.findByName("Body Temperature").orElse(null);
        PhenomenonType heartRate  = ptRepo.findByName("Heart Rate").orElse(null);
        PhenomenonType glucose    = ptRepo.findByName("Blood Glucose").orElse(null);
        PhenomenonType weight     = ptRepo.findByName("Body Weight").orElse(null);
        PhenomenonType systolicBP = ptRepo.findByName("Systolic Blood Pressure").orElse(null);
        PhenomenonType oxygenSat  = ptRepo.findByName("Oxygen Saturation").orElse(null);

        // Rule 1 — CONJUNCTIVE
        if (afRepo.findByName("Systemic Inflammatory Response").isEmpty()
                && bodyTemp != null && heartRate != null && systolicBP != null) {
            AssociativeFunction rule1 = new AssociativeFunction();
            rule1.setName("Systemic Inflammatory Response");
            rule1.setStrategyHint("CONJUNCTIVE");
            rule1.setArgumentConcepts(Set.of(bodyTemp, heartRate));
            rule1.setProductConcept(systolicBP);
            rule1.setActive(true);
            afRepo.save(rule1);
        }

        // Rule 2 — WEIGHTED: glucose weight 0.7, body-weight weight 0.5, threshold 1.0
        if (afRepo.findByName("Metabolic Risk").isEmpty()
                && glucose != null && weight != null && systolicBP != null) {
            AssociativeFunction rule2 = new AssociativeFunction();
            rule2.setName("Metabolic Risk");
            rule2.setStrategyHint("WEIGHTED");
            rule2.setThreshold(1.0);
            rule2.setArgumentConcepts(Set.of(glucose, weight));
            rule2.setProductConcept(systolicBP);
            rule2.setActive(true);
            AssociativeFunction saved2 = afRepo.save(rule2);
            saveWeight(saved2, glucose, 0.7);
            saveWeight(saved2, weight,  0.5);
        }

        // Rule 3 — CONJUNCTIVE
        if (afRepo.findByName("Respiratory Compromise").isEmpty()
                && oxygenSat != null && heartRate != null && bodyTemp != null) {
            AssociativeFunction rule3 = new AssociativeFunction();
            rule3.setName("Respiratory Compromise");
            rule3.setStrategyHint("CONJUNCTIVE");
            rule3.setArgumentConcepts(Set.of(oxygenSat, heartRate));
            rule3.setProductConcept(bodyTemp);
            rule3.setActive(true);
            afRepo.save(rule3);
        }
    }

    private void saveWeight(AssociativeFunction rule, PhenomenonType pt, double w) {
        ArgumentWeight aw = new ArgumentWeight();
        aw.setRule(rule);
        aw.setPhenomenonType(pt);
        aw.setWeight(w);
        weightRepo.save(aw);
    }

    // ---- Protocols ----

    private void seedProtocols() {
        Object[][] data = {
            { "Standard Blood Pressure Protocol",  "Two readings 5 min apart, patient seated.",       AccuracyRating.HIGH   },
            { "Fasting Blood Glucose Protocol",    "Patient fasts 8 hrs prior to measurement.",        AccuracyRating.HIGH   },
            { "Oral Temperature Protocol",         "Thermometer under tongue for 3 minutes.",          AccuracyRating.MEDIUM },
            { "Pulse Oximetry Protocol",           "Probe on index finger, patient at rest.",           AccuracyRating.MEDIUM },
            { "Manual Heart Rate Protocol",        "Radial pulse counted for 60 seconds.",              AccuracyRating.MEDIUM },
            { "Random Blood Glucose Protocol",     "Sample taken regardless of meal timing.",           AccuracyRating.MEDIUM },
            { "Body Weight Protocol",              "Patient weighed in light clothing, no shoes.",      AccuracyRating.HIGH   },
            { "Tympanic Temperature Protocol",     "Ear thermometer, right ear preferred.",             AccuracyRating.LOW    },
            { "Visual Pain Assessment Protocol",   "Patient self-reports on numeric 0-10 scale.",       AccuracyRating.LOW    },
            { "Standard Observation Protocol",     "General clinical observation by trained staff.",    AccuracyRating.MEDIUM },
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