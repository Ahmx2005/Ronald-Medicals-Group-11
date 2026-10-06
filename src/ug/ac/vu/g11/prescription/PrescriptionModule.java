package ug.ac.vu.g11.prescription;

import ug.ac.vu.g11.core.Core.FileStorage;
import ug.ac.vu.g11.core.Core.InputHelper;
import ug.ac.vu.g11.core.Core.Record;
import ug.ac.vu.g11.core.Core.Reportable;
import ug.ac.vu.g11.patient.PatientModule.Patient;
import ug.ac.vu.g11.patient.PatientModule.PatientService;
import ug.ac.vu.g11.stock.StockModule.Medication;
import ug.ac.vu.g11.stock.StockModule.MedicationService;
import ug.ac.vu.g11.stock.StockModule.OutOfStockException;
import ug.ac.vu.g11.stock.StockModule.StockMovement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PRESCRIPTION module. Owner: Ahmed.
 *
 * A prescription is the record that a patient was given a medicine, how to take it, and how many.
 * Issuing a prescription also takes that many units out of the medication stock, so this module
 * works together with the Stock module.
 *
 * The file contains five classes. Read them in this order:
 *   DrugNotInStockException - the error we throw when a prescription detail is wrong or the medicine is missing
 *   PrescriptionItem        - one medicine, its dosage and quantity (a "model" class: it just holds data)
 *   Prescription            - one prescription (a "model" class that extends Record and links to a real Patient)
 *   PrescriptionService     - keeps the list of prescriptions, and adds, finds, updates and removes them
 *   PrescriptionMenu        - the screen the user sees for the Prescription module
 *
 * @author Ahmed
 */
public class PrescriptionModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". This one is thrown when a
     * prescription detail is invalid, or when the medicine does not exist.
     *
     * @author Ahmed
     */
    public static class DrugNotInStockException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user
         */
        public DrugNotInStockException(String message) {
            // "super" passes the message up to the Exception class, which stores it for later.
            super(message);
        }
    }

    /**
     * PrescriptionItem is one medicine on a prescription: which medicine, how to take it (dosage),
     * and how many to give out. It is a "model class" that checks its own values.
     *
     * Real-life picture: one line on a pharmacy label, such as "Paracetamol, 1 tablet twice a day, 2 packs".
     *
     * @author Ahmed
     */
    public static class PrescriptionItem {
        // The medicine from the stock list. This is an object reference to a Medication.
        private Medication medication;
        // The instructions, for example "1 tablet twice a day".
        private String dosage;
        // How many units to give out. Must be more than zero.
        private int quantity;

        /**
         * Creates one prescription line. Each value is checked by its setter.
         *
         * @param medication the medicine (must exist in stock)
         * @param dosage the instructions for taking it
         * @param quantity how many units to give out
         * @throws DrugNotInStockException if any value is invalid
         */
        public PrescriptionItem(Medication medication, String dosage, int quantity) throws DrugNotInStockException {
            setMedication(medication);
            setDosage(dosage);
            setQuantity(quantity);
        }

        /**
         * Gives back the medicine.
         *
         * @return the Medication object
         */
        public Medication getMedication() {
            return medication;
        }

        /**
         * Links the item to a medicine. A prescription cannot name a medicine that does not exist.
         *
         * @param medication the medicine to link
         * @throws DrugNotInStockException if the medication is null
         */
        public void setMedication(Medication medication) throws DrugNotInStockException {
            if (medication == null) {
                throw new DrugNotInStockException("That medication does not exist in stock.");
            }
            this.medication = medication;
        }

        /**
         * Gives back the dosage instructions.
         *
         * @return the dosage text, for example "1 tablet twice a day"
         */
        public String getDosage() {
            return dosage;
        }

        /**
         * Changes the dosage instructions, but only if they are not blank.
         *
         * @param dosage the new instructions
         * @throws DrugNotInStockException if the dosage is null, empty or only spaces
         */
        public void setDosage(String dosage) throws DrugNotInStockException {
            if (dosage == null || dosage.trim().isEmpty()) {
                throw new DrugNotInStockException("Dosage instructions cannot be empty.");
            }
            this.dosage = dosage;
        }

        /**
         * Gives back the quantity.
         *
         * @return how many units are given out
         */
        public int getQuantity() {
            return quantity;
        }

        /**
         * Changes the quantity, but only if it is more than zero. Giving out zero, or a negative number,
         * makes no sense.
         *
         * @param quantity the new quantity
         * @throws DrugNotInStockException if the quantity is zero or below
         */
        public void setQuantity(int quantity) throws DrugNotInStockException {
            if (quantity <= 0) {
                throw new DrugNotInStockException("Quantity must be more than zero.");
            }
            this.quantity = quantity;
        }

        /**
         * Turns the item into text for saving. The "~" symbol separates the three parts.
         * Only the medication's ID is saved, not the whole medication.
         * Example: G11-MD001~1 tablet twice a day~2
         *
         * @return the text form of this item
         */
        public String toLine() {
            return medication.getId() + "~" + dosage + "~" + quantity;
        }

        /**
         * Rebuilds a prescription item from its saved text. It looks up the medicine by ID.
         *
         * @param text the saved text, for example "G11-MD001~1 tablet twice a day~2"
         * @param medicationService the stock list, used to find the medicine by ID
         * @return a new PrescriptionItem made from the text
         * @throws DrugNotInStockException if the medicine no longer exists or a value is invalid
         */
        public static PrescriptionItem fromLine(String text, MedicationService medicationService)
                throws DrugNotInStockException {
            // Cut the text at each ~. parts[0] is the medicine ID, parts[1] the dosage, parts[2] the quantity.
            String[] parts = text.split("~", -1);
            // Integer.parseInt turns the text "2" into the number 2.
            return new PrescriptionItem(medicationService.findById(parts[0]), parts[1], Integer.parseInt(parts[2]));
        }
    }

    /**
     * Prescription is one medicine given to one patient. It extends Record (so it has an ID and describe())
     * and implements Reportable (so it has getSummary()).
     *
     * @author Ahmed
     */
    public static class Prescription extends Record implements Reportable {
        // The patient who receives the medicine. An object reference, so the patient's name is always current.
        private Patient patient;
        // The medicine details. Each prescription holds exactly one item in this version.
        private PrescriptionItem item;

        /**
         * Creates a new prescription.
         *
         * @param id the unique ID, for example G11-RX001
         * @param patient the real patient receiving the medicine
         * @param item the medicine, dosage and quantity
         * @throws DrugNotInStockException if the patient is missing
         */
        public Prescription(String id, Patient patient, PrescriptionItem item) throws DrugNotInStockException {
            // Runs the Record constructor first, which sets the ID.
            super(id);
            setPatient(patient);
            // The item has already been checked when it was built, so it can be stored directly.
            this.item = item;
        }

        /**
         * Gives back the patient.
         *
         * @return the linked Patient object
         */
        public Patient getPatient() {
            return patient;
        }

        /**
         * Links the prescription to a patient. A prescription cannot exist without a real patient.
         *
         * @param patient the patient to link
         * @throws DrugNotInStockException if the patient is null
         */
        public void setPatient(Patient patient) throws DrugNotInStockException {
            if (patient == null) {
                throw new DrugNotInStockException("A prescription must be linked to a real patient.");
            }
            this.patient = patient;
        }

        /**
         * Gives back the medicine item on this prescription.
         *
         * @return the PrescriptionItem object
         */
        public PrescriptionItem getItem() {
            return item;
        }

        /**
         * Turns the prescription into one line for the file. Only IDs are saved for the patient and medicine.
         * Example: G11-RX001|G11-PT001|G11-MD001~1 tablet twice a day~2
         *
         * @return the one-line text form of this prescription
         */
        public String toLine() {
            return getId() + "|" + patient.getId() + "|" + item.toLine();
        }

        /**
         * Rebuilds a prescription from one line of the file. It finds the patient and the medicine by ID.
         *
         * @param line one saved line from prescriptions.txt
         * @param patientService the patient list, used to find the patient
         * @param medicationService the stock list, used to find the medicine
         * @return a new Prescription made from the line
         * @throws DrugNotInStockException if a value is missing or invalid
         */
        public static Prescription fromLine(String line, PatientService patientService,
                                            MedicationService medicationService) throws DrugNotInStockException {
            // parts[2] is the medicine part of the line, which is handed to PrescriptionItem.fromLine.
            String[] parts = line.split("\\|", -1);
            return new Prescription(parts[0], patientService.findById(parts[1]),
                    PrescriptionItem.fromLine(parts[2], medicationService));
        }

        /**
         * Describes the prescription in a full sentence. Required by the Record parent class.
         * Example: "Prescription G11-RX001: Ali Test - Paracetamol x2 (1 tablet twice a day)".
         *
         * @return the sentence describing this prescription
         */
        @Override
        public String describe() {
            return "Prescription " + getId() + ": " + patient.getName() + " - " + item.getMedication().getName()
                    + " x" + item.getQuantity() + " (" + item.getDosage() + ")";
        }

        /**
         * Gives a short one-line summary, used in lists.
         *
         * @return a short summary, for example "G11-RX001 | Ali Test | Paracetamol"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + patient.getName() + " | " + item.getMedication().getName();
        }
    }

    /**
     * PrescriptionService is the "manager" of all prescriptions. It keeps every prescription in an ArrayList,
     * loads the list from prescriptions.txt when the program starts, and saves after every change.
     *
     * @author Ahmed
     */
    public static class PrescriptionService {
        // The file where prescriptions are saved, and the text every prescription ID starts with.
        private static final String FILE_NAME = "prescriptions.txt";
        private static final String ID_PREFIX = "G11-RX";

        // The list of prescriptions, the next ID number, and the two services needed to rebuild them.
        private List<Prescription> prescriptions = new ArrayList<>();
        private int nextNumber = 1;
        private PatientService patientService;
        private MedicationService medicationService;

        /**
         * Creates the service and loads the saved prescriptions.
         *
         * @param patientService the patient service, used to link each prescription to its patient
         * @param medicationService the stock service, used to link each prescription to its medicine
         */
        public PrescriptionService(PatientService patientService, MedicationService medicationService) {
            this.patientService = patientService;
            this.medicationService = medicationService;
            load();
        }

        /**
         * Reads prescriptions.txt and rebuilds each prescription. A damaged line is skipped with a message.
         * Then it works out the next free ID number.
         */
        private void load() {
            for (String line : FileStorage.readLines(FILE_NAME)) {
                try {
                    prescriptions.add(Prescription.fromLine(line, patientService, medicationService));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged prescription record in the file.");
                }
            }
            // Find the biggest number already used, so new IDs never clash with old ones.
            for (Prescription p : prescriptions) {
                try {
                    int number = Integer.parseInt(p.getId().replace(ID_PREFIX, ""));
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Writes every prescription to prescriptions.txt.
         */
        private void save() {
            List<String> lines = new ArrayList<>();
            for (Prescription p : prescriptions) {
                lines.add(p.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next prescription ID and moves the counter on.
         *
         * @return the new ID, for example G11-RX001
         */
        public String generateId() {
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            nextNumber++;
            return id;
        }

        /**
         * Adds a new prescription to the list and saves the file.
         *
         * @param prescription the prescription to add
         */
        public void add(Prescription prescription) {
            prescriptions.add(prescription);
            save();
        }

        /**
         * Gives back the whole list of prescriptions.
         *
         * @return the list of all prescriptions
         */
        public List<Prescription> getAll() {
            return prescriptions;
        }

        /**
         * Finds one prescription by ID. Upper and lower case do not matter.
         *
         * @param id the ID to look for
         * @return the matching prescription, or null if none has that ID
         */
        public Prescription findById(String id) {
            for (Prescription p : prescriptions) {
                if (p.getId().equalsIgnoreCase(id)) {
                    return p;
                }
            }
            return null;
        }

        /**
         * Removes a prescription by ID and saves the file. Note that this does not put the medicine
         * back into stock.
         *
         * @param id the ID of the prescription to remove
         * @return true if it was removed, false if no prescription has that ID
         */
        public boolean remove(String id) {
            Prescription prescription = findById(id);
            if (prescription == null) {
                return false;
            }
            prescriptions.remove(prescription);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after changing a prescription, such as its dosage.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the prescriptions, from A to Z by the patient's name.
         * The original list is not changed.
         *
         * @return a new list of prescriptions sorted by patient name
         */
        public List<Prescription> sortedByPatientName() {
            List<Prescription> copy = new ArrayList<>(prescriptions);
            // This "lambda" (a short unnamed function) says: for each prescription, sort by its patient's name.
            copy.sort(Comparator.comparing(p -> p.getPatient().getName()));
            return copy;
        }
    }

    /**
     * PrescriptionMenu is the screen the user sees for the Prescription module. It shows the options,
     * reads the choice, and calls the right action.
     *
     * @author Ahmed
     */
    public static class PrescriptionMenu {
        // The prescription manager, plus the patient and stock managers used to check IDs and take stock out.
        private PrescriptionService service;
        private PatientService patientService;
        private MedicationService medicationService;

        /**
         * Creates the menu.
         *
         * @param service the prescription service that keeps the prescriptions
         * @param patientService the patient service, used to check patient IDs
         * @param medicationService the stock service, used to check medicine IDs and take stock out
         */
        public PrescriptionMenu(PrescriptionService service, PatientService patientService,
                                MedicationService medicationService) {
            this.service = service;
            this.patientService = patientService;
            this.medicationService = medicationService;
        }

        /**
         * Shows the Prescription menu over and over, until the user picks 0.
         */
        public void show() {
            boolean running = true;
            while (running) {
                int choice = InputHelper.showMenu("PRESCRIPTION", "Back to main menu",
                        "Issue a new prescription",
                        "Change a prescription's dosage",
                        "Remove a prescription",
                        "Prescription report (sorted by patient name)");

                // Run the action that matches the number the user typed.
                switch (choice) {
                    case 1: issue(); break;
                    case 2: changeDosage(); break;
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
        // OPTION 1: Issue a new prescription
        // ============================================================
        /**
         * Issues a prescription. It checks the patient and the medicine, builds the item, takes the stock
         * out, and then saves. If any step fails, the stock is not changed.
         */
        private void issue() {
            String patientId = InputHelper.readNonEmptyText("Patient ID: ");
            Patient patient = patientService.findById(patientId);
            if (patient == null) {
                System.out.println("No patient found with that ID.");
                return;
            }
            String medicationId = InputHelper.readNonEmptyText("Medication ID: ");
            Medication medication = medicationService.findById(medicationId);
            if (medication == null) {
                System.out.println("No medication found with that ID.");
                return;
            }
            String dosage = InputHelper.readNonEmptyText("Dosage instructions (e.g. 1 tablet twice a day): ");
            int quantity = InputHelper.readInt("Quantity to give out: ");
            // "try" catches two kinds of error: not enough stock (OutOfStockException) and a bad item (DrugNotInStockException).
            try {
                // Build the item first. If its values are wrong, we stop before touching the stock.
                PrescriptionItem item = new PrescriptionItem(medication, dosage, quantity);
                // Take the units out of stock. This throws an error if there are not enough.
                medication.dispense(quantity);
                medicationService.update();
                // Print a short line showing the stock change, for example "DISPENSE 2 of Paracetamol".
                System.out.println("Recorded: " + new StockMovement(medication.getName(), "DISPENSE", quantity));
                String id = service.generateId();
                service.add(new Prescription(id, patient, item));
                System.out.println("Prescription issued with ID " + id);
            } catch (OutOfStockException | DrugNotInStockException e) {
                // The "|" here means "either of these error types", so one catch handles both.
                System.out.println("Could not issue prescription: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Change a prescription's dosage
        // ============================================================
        /**
         * Finds a prescription by ID and changes the dosage instructions on it.
         */
        private void changeDosage() {
            String id = InputHelper.readNonEmptyText("Enter prescription ID: ");
            Prescription prescription = service.findById(id);
            if (prescription == null) {
                System.out.println("No prescription found with that ID.");
                return;
            }
            try {
                // Go to the item inside the prescription, change its dosage (checked by the setter), then save.
                prescription.getItem().setDosage(InputHelper.readNonEmptyText("New dosage instructions: "));
                service.update();
                System.out.println("Dosage updated.");
            } catch (DrugNotInStockException e) {
                System.out.println("Could not update: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 3: Remove a prescription
        // ============================================================
        /**
         * Removes a prescription by ID.
         */
        private void remove() {
            String id = InputHelper.readNonEmptyText("Enter prescription ID to remove: ");
            System.out.println(service.remove(id) ? "Prescription removed." : "No prescription found with that ID.");
        }

        // ============================================================
        // OPTION 4: Prescription report (sorted by patient name)
        // ============================================================
        /**
         * Prints every prescription, sorted by the patient's name.
         */
        private void report() {
            List<Prescription> sorted = service.sortedByPatientName();
            if (sorted.isEmpty()) {
                System.out.println("No prescriptions to report.");
                return;
            }
            System.out.println("--- Prescription Report (sorted by patient name) ---");
            // Each prescription is a Record, so describe() works on it.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }
    }
}
