package ug.ac.vu.g11.triage;

import ug.ac.vu.g11.core.Core.FileStorage;
import ug.ac.vu.g11.core.Core.InputHelper;
import ug.ac.vu.g11.core.Core.Record;
import ug.ac.vu.g11.core.Core.Reportable;
import ug.ac.vu.g11.patient.PatientModule.Patient;
import ug.ac.vu.g11.patient.PatientModule.PatientService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TRIAGE / CONSULTATION module. Owner: David.
 *
 * "Triage" means checking a patient's condition when they arrive, and "consultation" means the
 * visit where a doctor sees the patient. This module records the temperature taken at the start
 * of a visit and the diagnosis the doctor writes at the end. Each visit gets an ID such as G11-CN001.
 *
 * The file contains five classes. Read them in this order:
 *   InvalidVitalSignException - the error we throw when a temperature or diagnosis is wrong
 *   VitalSigns                - the temperature taken at triage (a "model" class: it just holds data)
 *   Consultation              - one visit (a "model" class that extends Record and links to a real Patient)
 *   ConsultationService       - keeps the list of visits, and adds, finds, updates and removes them
 *   TriageMenu                - the screen the user sees for the Consultation module
 *
 * @author David
 */
public class TriageModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". Throwing it stops the action
     * and carries a message that explains the problem.
     *
     * @author David
     */
    public static class InvalidVitalSignException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user
         */
        public InvalidVitalSignException(String message) {
            // "super" passes the message up to the Exception class, which stores it for later.
            super(message);
        }
    }

    /**
     * VitalSigns holds the body temperature taken at triage. It is a "model class": a small class
     * whose job is to hold one piece of data and refuse impossible values.
     *
     * @author David
     */
    public static class VitalSigns {
        // "private" means only this class can change the temperature directly.
        private double temperatureCelsius;

        /**
         * Creates a temperature reading. The value must be between 30 and 45 degrees Celsius.
         *
         * @param temperatureCelsius the temperature, for example 37.5
         * @throws InvalidVitalSignException if the temperature is outside 30 to 45
         */
        public VitalSigns(double temperatureCelsius) throws InvalidVitalSignException {
            // Going through the setter means the same check always runs.
            setTemperatureCelsius(temperatureCelsius);
        }

        /**
         * Gives back the temperature.
         *
         * @return the temperature in degrees Celsius
         */
        public double getTemperatureCelsius() {
            return temperatureCelsius;
        }

        /**
         * Changes the temperature, but only if it is between 30 and 45 degrees Celsius.
         * A human body is never outside that range, so a value outside it is almost certainly a typing mistake.
         *
         * @param temperatureCelsius the new temperature
         * @throws InvalidVitalSignException if the temperature is below 30 or above 45
         */
        public void setTemperatureCelsius(double temperatureCelsius) throws InvalidVitalSignException {
            // "||" means "or": the value is refused if it is too low OR too high.
            if (temperatureCelsius < 30 || temperatureCelsius > 45) {
                throw new InvalidVitalSignException("Temperature must be between 30 and 45 degrees Celsius.");
            }
            this.temperatureCelsius = temperatureCelsius;
        }
    }

    /**
     * One visit where a doctor saw a patient. It extends Record (so it has an ID and describe())
     * and implements Reportable (so it has getSummary()).
     *
     * The consultation holds a reference to the real Patient object, not a copy of the name.
     *
     * @author David
     */
    public static class Consultation extends Record implements Reportable {
        // The patient this visit is for. This is an object reference, which links the two modules together.
        private Patient patient;
        // The temperature reading for this visit. It is an object too, so it can check its own values.
        private VitalSigns vitalSigns;
        // What the doctor concluded, for example "Fever" or "Malaria".
        private String diagnosis;

        /**
         * Creates a new visit. Each value is checked by its setter.
         *
         * @param id the unique ID, for example G11-CN001
         * @param patient the real patient who was seen
         * @param vitalSigns the temperature reading
         * @param diagnosis what the doctor concluded
         * @throws InvalidVitalSignException if the patient or diagnosis is missing
         */
        public Consultation(String id, Patient patient, VitalSigns vitalSigns, String diagnosis)
                throws InvalidVitalSignException {
            // Runs the Record constructor first, which sets the ID.
            super(id);
            setPatient(patient);
            // The VitalSigns object has already been checked when it was built, so we store it directly.
            this.vitalSigns = vitalSigns;
            setDiagnosis(diagnosis);
        }

        /**
         * Gives back the patient for this visit.
         *
         * @return the linked Patient object
         */
        public Patient getPatient() {
            return patient;
        }

        /**
         * Links the visit to a patient. A visit cannot exist without a real patient.
         *
         * @param patient the patient to link
         * @throws InvalidVitalSignException if the patient is null
         */
        public void setPatient(Patient patient) throws InvalidVitalSignException {
            if (patient == null) {
                throw new InvalidVitalSignException("A consultation must be linked to a real patient.");
            }
            this.patient = patient;
        }

        /**
         * Gives back the diagnosis.
         *
         * @return the diagnosis text
         */
        public String getDiagnosis() {
            return diagnosis;
        }

        /**
         * Changes the diagnosis, but only if it is not blank.
         *
         * @param diagnosis the new diagnosis
         * @throws InvalidVitalSignException if the diagnosis is null, empty or only spaces
         */
        public void setDiagnosis(String diagnosis) throws InvalidVitalSignException {
            if (diagnosis == null || diagnosis.trim().isEmpty()) {
                throw new InvalidVitalSignException("Diagnosis cannot be empty.");
            }
            this.diagnosis = diagnosis;
        }

        /**
         * Turns the visit into one line for the file. Only the patient's ID is saved, not the whole patient.
         * Example: G11-CN001|G11-PT001|37.5|Fever
         *
         * @return the one-line text form of this visit
         */
        public String toLine() {
            // getTemperatureCelsius() returns a number, and "+" turns it into text automatically when joining.
            return getId() + "|" + patient.getId() + "|" + vitalSigns.getTemperatureCelsius() + "|" + diagnosis;
        }

        /**
         * Rebuilds a visit from one line of the file. It looks up the real patient by ID, because the
         * file only stores the ID.
         *
         * @param line one saved line from consultations.txt
         * @param patientService the patient list, used to find the patient by ID
         * @return a new Consultation made from the line
         * @throws InvalidVitalSignException if the patient no longer exists or a value is invalid
         */
        public static Consultation fromLine(String line, PatientService patientService) throws InvalidVitalSignException {
            // Cut the line at each | into pieces. parts[2] is the temperature text, for example "37.5".
            String[] parts = line.split("\\|", -1);
            Patient patient = patientService.findById(parts[1]);
            if (patient == null) {
                throw new InvalidVitalSignException("Patient for this consultation no longer exists.");
            }
            // Double.parseDouble turns the text "37.5" into the number 37.5.
            return new Consultation(parts[0], patient,
                    new VitalSigns(Double.parseDouble(parts[2])), parts[3]);
        }

        /**
         * Describes the visit in a full sentence. Required by the Record parent class.
         * Example: "Consultation G11-CN001: Ali Test - diagnosis: Fever".
         *
         * @return the sentence describing this visit
         */
        @Override
        public String describe() {
            return "Consultation " + getId() + ": " + patient.getName() + " - diagnosis: " + diagnosis;
        }

        /**
         * Gives a short one-line summary, used in lists.
         *
         * @return a short summary, for example "G11-CN001 | Ali Test | Fever"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + patient.getName() + " | " + diagnosis;
        }
    }

    /**
     * ConsultationService is the "manager" of all visits. It keeps every visit in an ArrayList,
     * loads the list from consultations.txt when the program starts, and saves after every change.
     *
     * @author David
     */
    public static class ConsultationService {
        // The file where visits are saved, and the text every consultation ID starts with.
        private static final String FILE_NAME = "consultations.txt";
        private static final String ID_PREFIX = "G11-CN";

        // The list of all visits, the next ID number to use, and the patient list (needed when loading).
        private List<Consultation> consultations = new ArrayList<>();
        private int nextNumber = 1;
        private PatientService patientService;

        /**
         * Creates the service and loads the saved visits.
         *
         * @param patientService the patient service, needed to link each visit to its patient
         */
        public ConsultationService(PatientService patientService) {
            this.patientService = patientService;
            load();
        }

        /**
         * Reads consultations.txt and rebuilds each visit. A damaged line, or one whose patient is missing,
         * is skipped with a message. Then it works out the next free ID number.
         */
        private void load() {
            for (String line : FileStorage.readLines(FILE_NAME)) {
                try {
                    consultations.add(Consultation.fromLine(line, patientService));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged consultation record in the file.");
                }
            }
            // Find the biggest number already used, so new IDs never clash with old ones.
            for (Consultation c : consultations) {
                try {
                    int number = Integer.parseInt(c.getId().replace(ID_PREFIX, ""));
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Writes every visit to consultations.txt.
         */
        private void save() {
            List<String> lines = new ArrayList<>();
            for (Consultation c : consultations) {
                lines.add(c.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next consultation ID and moves the counter on.
         *
         * @return the new ID, for example G11-CN001
         */
        public String generateId() {
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            nextNumber++;
            return id;
        }

        /**
         * Adds a new visit to the list and saves the file.
         *
         * @param consultation the visit to add
         */
        public void add(Consultation consultation) {
            consultations.add(consultation);
            save();
        }

        /**
         * Gives back the whole list of visits.
         *
         * @return the list of all consultations
         */
        public List<Consultation> getAll() {
            return consultations;
        }

        /**
         * Finds one visit by ID. Upper and lower case do not matter.
         *
         * @param id the ID to look for
         * @return the matching visit, or null if none has that ID
         */
        public Consultation findById(String id) {
            for (Consultation c : consultations) {
                if (c.getId().equalsIgnoreCase(id)) {
                    return c;
                }
            }
            return null;
        }

        /**
         * Removes a visit by ID and saves the file.
         *
         * @param id the ID of the visit to remove
         * @return true if it was removed, false if no visit has that ID
         */
        public boolean remove(String id) {
            Consultation consultation = findById(id);
            if (consultation == null) {
                return false;
            }
            consultations.remove(consultation);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after changing a visit, such as its diagnosis.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the visits, from A to Z by diagnosis. The original list is not changed.
         *
         * @return a new list of visits sorted by diagnosis
         */
        public List<Consultation> sortedByDiagnosis() {
            List<Consultation> copy = new ArrayList<>(consultations);
            // Comparator.comparing names the field to sort by: the diagnosis text.
            copy.sort(Comparator.comparing(Consultation::getDiagnosis));
            return copy;
        }
    }

    /**
     * TriageMenu is the screen the user sees for the Consultation module. It shows the options,
     * reads the choice, and calls the right action.
     *
     * @author David
     */
    public static class TriageMenu {
        // The visit manager, and the patient manager (used to check that a patient exists).
        private ConsultationService service;
        private PatientService patientService;

        /**
         * Creates the menu.
         *
         * @param service the consultation service that keeps the visits
         * @param patientService the patient service, used to check patient IDs
         */
        public TriageMenu(ConsultationService service, PatientService patientService) {
            this.service = service;
            this.patientService = patientService;
        }

        /**
         * Shows the Consultation menu over and over, until the user picks 0.
         */
        public void show() {
            boolean running = true;
            while (running) {
                int choice = InputHelper.showMenu("CONSULTATION", "Back to main menu",
                        "Record a new consultation",
                        "Change a diagnosis",
                        "Remove a consultation",
                        "Consultation report (sorted by diagnosis)");

                // Run the action that matches the number the user typed.
                switch (choice) {
                    case 1: record(); break;
                    case 2: changeDiagnosis(); break;
                    case 3: remove(); break;
                    case 4: report(); break;
                    case 0: running = false; break;
                    default: System.out.println("Please choose a number from the menu.");
                }
                // Wait for Enter after an action, so the user can read the result.
                if (running) {
                    InputHelper.pressEnter();
                }
            }
        }

        // ============================================================
        // OPTION 1: Record a new consultation
        // ============================================================
        /**
         * Asks for the patient, the temperature and the diagnosis, then saves the visit.
         */
        private void record() {
            String patientId = InputHelper.readNonEmptyText("Patient ID: ");
            Patient patient = patientService.findById(patientId);
            if (patient == null) {
                System.out.println("No patient found with that ID.");
                return;
            }
            // readDouble keeps asking until a real number is typed, so "abc" can never reach the program.
            double temp = InputHelper.readDouble("Temperature (Celsius): ");
            String diagnosis = InputHelper.readNonEmptyText("Diagnosis: ");
            try {
                // The VitalSigns object checks the temperature range when it is created.
                String id = service.generateId();
                service.add(new Consultation(id, patient, new VitalSigns(temp), diagnosis));
                System.out.println("Consultation recorded with ID " + id);
            } catch (InvalidVitalSignException e) {
                System.out.println("Could not record consultation: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Change a diagnosis
        // ============================================================
        /**
         * Finds a visit by ID and changes its diagnosis.
         */
        private void changeDiagnosis() {
            String id = InputHelper.readNonEmptyText("Enter consultation ID: ");
            Consultation consultation = service.findById(id);
            if (consultation == null) {
                System.out.println("No consultation found with that ID.");
                return;
            }
            try {
                // Ask for the new diagnosis, store it on the visit (checked by the setter), then save.
                consultation.setDiagnosis(InputHelper.readNonEmptyText("New diagnosis: "));
                service.update();
                System.out.println("Diagnosis updated.");
            } catch (InvalidVitalSignException e) {
                System.out.println("Could not update: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 3: Remove a consultation
        // ============================================================
        /**
         * Removes a visit by ID.
         */
        private void remove() {
            String id = InputHelper.readNonEmptyText("Enter consultation ID to remove: ");
            System.out.println(service.remove(id) ? "Consultation removed." : "No consultation found with that ID.");
        }

        // ============================================================
        // OPTION 4: Consultation report (sorted by diagnosis)
        // ============================================================
        /**
         * Prints every visit, sorted by diagnosis.
         */
        private void report() {
            List<Consultation> sorted = service.sortedByDiagnosis();
            if (sorted.isEmpty()) {
                System.out.println("No consultations to report.");
                return;
            }
            System.out.println("--- Consultation Report (sorted by diagnosis) ---");
            // Each visit is a Record, so describe() works on it.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }
    }
}
