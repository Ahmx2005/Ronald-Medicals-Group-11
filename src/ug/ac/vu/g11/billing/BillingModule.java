package ug.ac.vu.g11.billing;

import ug.ac.vu.g11.core.Core.FileStorage;
import ug.ac.vu.g11.core.Core.InputHelper;
import ug.ac.vu.g11.core.Core.Record;
import ug.ac.vu.g11.core.Core.Reportable;
import ug.ac.vu.g11.labtest.LabTestModule.LabTest;
import ug.ac.vu.g11.labtest.LabTestModule.LabTestService;
import ug.ac.vu.g11.patient.PatientModule.Patient;
import ug.ac.vu.g11.patient.PatientModule.PatientService;
import ug.ac.vu.g11.prescription.PrescriptionModule.Prescription;
import ug.ac.vu.g11.prescription.PrescriptionModule.PrescriptionService;
import ug.ac.vu.g11.triage.TriageModule.Consultation;
import ug.ac.vu.g11.triage.TriageModule.ConsultationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BILLING module. Owner: Ethar.
 *
 * An invoice is the bill a patient is asked to pay. This module is the "hub" of the system: an invoice
 * can point at the real consultation, prescription and lab test that it charges for, so it links
 * together data from four other modules. Each invoice gets an ID such as G11-BL001.
 *
 * The file contains five classes. Read them in this order:
 *   InvalidPaymentException - the error we throw when an invoice or payment detail is wrong
 *   InvoiceItem             - one line on a receipt (a "model" class: it just holds data)
 *   Invoice                 - one bill for one patient (a "model" class that extends Record)
 *   BillingService          - keeps the list of invoices, and adds, finds, updates and removes them
 *   BillingMenu             - the screen the user sees for the Billing module
 *
 * @author Ethar
 */
public class BillingModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". This one is thrown when an invoice
     * or receipt detail is invalid.
     *
     * @author Ethar
     */
    public static class InvalidPaymentException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user
         */
        public InvalidPaymentException(String message) {
            // "super" passes the message up to the Exception class, which stores it for later.
            super(message);
        }
    }

    /**
     * InvoiceItem is one printed line on a receipt, such as "Total charge for Ali Test: 150.0".
     * It is a "model class": it holds a description and an amount, and checks both.
     *
     * @author Ethar
     */
    public static class InvoiceItem {
        // What the line is for, and how much it costs.
        private String description;
        private double amount;

        /**
         * Creates a receipt line. Both values are checked by their setters.
         *
         * @param description what the line is for
         * @param amount how much it costs, zero or more
         * @throws InvalidPaymentException if the description is blank or the amount is negative
         */
        public InvoiceItem(String description, double amount) throws InvalidPaymentException {
            setDescription(description);
            setAmount(amount);
        }

        /**
         * Changes the description, but only if it is not blank.
         *
         * @param description the new description
         * @throws InvalidPaymentException if the description is null, empty or only spaces
         */
        public void setDescription(String description) throws InvalidPaymentException {
            if (description == null || description.trim().isEmpty()) {
                throw new InvalidPaymentException("Receipt line description cannot be empty.");
            }
            this.description = description;
        }

        /**
         * Gives back the amount of this line.
         *
         * @return the amount
         */
        public double getAmount() {
            return amount;
        }

        /**
         * Changes the amount, but only if it is zero or more. A bill line cannot cost less than nothing.
         *
         * @param amount the new amount
         * @throws InvalidPaymentException if the amount is negative
         */
        public void setAmount(double amount) throws InvalidPaymentException {
            if (amount < 0) {
                throw new InvalidPaymentException("Receipt line amount cannot be negative.");
            }
            this.amount = amount;
        }

        /**
         * Shows the line as text, for example "Total charge for Ali Test: 150.0".
         *
         * @return the description and amount joined by a colon
         */
        @Override
        public String toString() {
            return description + ": " + amount;
        }
    }

    /**
     * Invoice is one bill for one patient. It extends Record (so it has an ID and describe())
     * and implements Reportable (so it has getSummary()).
     *
     * The billed items list can hold a mix of consultations, prescriptions and lab tests. Because each
     * of those is a Record, the list is typed as List of Record, and the loop in the menu can call
     * describe() on any of them.
     *
     * @author Ethar
     */
    public static class Invoice extends Record implements Reportable {
        // The patient being billed.
        private Patient patient;
        // The things this invoice charges for. A List of Record can hold any mix of record types.
        private List<Record> billedItems;
        // The total amount to pay.
        private double amount;

        /**
         * Creates a new invoice.
         *
         * @param id the unique ID, for example G11-BL001
         * @param patient the real patient being billed
         * @param billedItems the consultations, prescriptions and lab tests being charged for (may be null)
         * @param amount the total amount to pay
         * @throws InvalidPaymentException if the patient is missing or the amount is negative
         */
        public Invoice(String id, Patient patient, List<Record> billedItems, double amount)
                throws InvalidPaymentException {
            // Runs the Record constructor first, which sets the ID.
            super(id);
            setPatient(patient);
            // "(condition) ? a : b": if there is no list given, use a new empty one, so later code never meets null.
            this.billedItems = (billedItems == null) ? new ArrayList<>() : billedItems;
            setAmount(amount);
        }

        /**
         * Gives back the patient being billed.
         *
         * @return the linked Patient object
         */
        public Patient getPatient() {
            return patient;
        }

        /**
         * Links the invoice to a patient. An invoice cannot exist without a real patient.
         *
         * @param patient the patient to link
         * @throws InvalidPaymentException if the patient is null
         */
        public void setPatient(Patient patient) throws InvalidPaymentException {
            if (patient == null) {
                throw new InvalidPaymentException("An invoice must be linked to a real patient.");
            }
            this.patient = patient;
        }

        /**
         * Gives back the list of things this invoice charges for.
         *
         * @return the list of billed records
         */
        public List<Record> getBilledItems() {
            return billedItems;
        }

        /**
         * Gives back the total amount.
         *
         * @return the amount to pay
         */
        public double getAmount() {
            return amount;
        }

        /**
         * Changes the total amount, but only if it is zero or more.
         *
         * @param amount the new total
         * @throws InvalidPaymentException if the amount is negative
         */
        public void setAmount(double amount) throws InvalidPaymentException {
            if (amount < 0) {
                throw new InvalidPaymentException("Invoice amount cannot be negative.");
            }
            this.amount = amount;
        }

        /**
         * Turns the invoice into one line for the file. The billed items are saved as tag:ID pairs,
         * where CN means consultation, RX means prescription and LT means lab test.
         * Example: G11-BL001|G11-PT001|CN:G11-CN001,RX:G11-RX001|200.0
         *
         * @return the one-line text form of this invoice
         */
        public String toLine() {
            // StringBuilder is a tool for joining many pieces of text efficiently.
            StringBuilder itemsPart = new StringBuilder();
            for (Record item : billedItems) {
                // Put a comma before every item except the first one.
                if (itemsPart.length() > 0) {
                    itemsPart.append(",");
                }
                // "instanceof" checks what kind of object the item really is, and picks the right tag.
                if (item instanceof Consultation) {
                    itemsPart.append("CN:").append(item.getId());
                } else if (item instanceof Prescription) {
                    itemsPart.append("RX:").append(item.getId());
                } else if (item instanceof LabTest) {
                    itemsPart.append("LT:").append(item.getId());
                }
            }
            return getId() + "|" + patient.getId() + "|" + itemsPart + "|" + amount;
        }

        /**
         * Describes the invoice in a full sentence. Required by the Record parent class.
         * Example: "Invoice G11-BL001: Ali Test - total 200.0".
         *
         * @return the sentence describing this invoice
         */
        @Override
        public String describe() {
            return "Invoice " + getId() + ": " + patient.getName() + " - total " + amount;
        }

        /**
         * Gives a short one-line summary, used in lists.
         *
         * @return a short summary, for example "G11-BL001 | Ali Test | 200.0"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + patient.getName() + " | " + amount;
        }
    }

    /**
     * BillingService is the "manager" of all invoices. It keeps every invoice in an ArrayList,
     * loads the list from invoices.txt when the program starts, and saves after every change.
     * It needs the other services too, because each invoice points at records from them.
     *
     * @author Ethar
     */
    public static class BillingService {
        // The file where invoices are saved, and the text every invoice ID starts with.
        private static final String FILE_NAME = "invoices.txt";
        private static final String ID_PREFIX = "G11-BL";

        // The list of invoices, the next ID number, and the four services used to look up linked records.
        private List<Invoice> invoices = new ArrayList<>();
        private int nextNumber = 1;
        private PatientService patientService;
        private ConsultationService consultationService;
        private PrescriptionService prescriptionService;
        private LabTestService labTestService;

        /**
         * Creates the service and loads the saved invoices.
         *
         * @param patientService the patient service, used to find each invoice's patient
         * @param consultationService the consultation service, used to find billed visits
         * @param prescriptionService the prescription service, used to find billed prescriptions
         * @param labTestService the lab test service, used to find billed lab tests
         */
        public BillingService(PatientService patientService, ConsultationService consultationService,
                              PrescriptionService prescriptionService, LabTestService labTestService) {
            this.patientService = patientService;
            this.consultationService = consultationService;
            this.prescriptionService = prescriptionService;
            this.labTestService = labTestService;
            load();
        }

        /**
         * Reads invoices.txt and rebuilds each invoice. A damaged line is skipped with a message.
         * Then it works out the next free ID number.
         */
        private void load() {
            for (String line : FileStorage.readLines(FILE_NAME)) {
                try {
                    invoices.add(fromLine(line));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged invoice record in the file.");
                }
            }
            // Find the biggest number already used, so new IDs never clash with old ones.
            for (Invoice i : invoices) {
                try {
                    int number = Integer.parseInt(i.getId().replace(ID_PREFIX, ""));
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Rebuilds one invoice from a saved line. It looks up the patient and every billed record by ID.
         * If a billed record has been deleted since the invoice was made, that link is left out
         * rather than causing a crash.
         *
         * @param line one saved line from invoices.txt
         * @return a new Invoice made from the line
         * @throws InvalidPaymentException if the patient is missing or a value is invalid
         */
        private Invoice fromLine(String line) throws InvalidPaymentException {
            // parts[2] holds the billed items, parts[3] holds the amount.
            String[] parts = line.split("\\|", -1);
            List<Record> items = new ArrayList<>();
            // An empty items part means nothing is billed, so there is nothing to look up.
            if (!parts[2].isEmpty()) {
                // Split "CN:G11-CN001,RX:G11-RX001" into the separate tag:ID pieces.
                for (String token : parts[2].split(",")) {
                    // Split one piece into its tag ("CN") and its ID ("G11-CN001").
                    String[] tagAndId = token.split(":");
                    // "Record item = null" means we have not found anything yet.
                    Record item = null;
                    if (tagAndId[0].equals("CN")) {
                        item = consultationService.findById(tagAndId[1]);
                    } else if (tagAndId[0].equals("RX")) {
                        item = prescriptionService.findById(tagAndId[1]);
                    } else if (tagAndId[0].equals("LT")) {
                        item = labTestService.findById(tagAndId[1]);
                    }
                    // Only add the item if it was found. A missing record is skipped, not added as null.
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            return new Invoice(parts[0], patientService.findById(parts[1]), items, Double.parseDouble(parts[3]));
        }

        /**
         * Writes every invoice to invoices.txt.
         */
        private void save() {
            List<String> lines = new ArrayList<>();
            for (Invoice i : invoices) {
                lines.add(i.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next invoice ID and moves the counter on.
         *
         * @return the new ID, for example G11-BL001
         */
        public String generateId() {
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            nextNumber++;
            return id;
        }

        /**
         * Adds a new invoice to the list and saves the file.
         *
         * @param invoice the invoice to add
         */
        public void add(Invoice invoice) {
            invoices.add(invoice);
            save();
        }

        /**
         * Gives back the whole list of invoices.
         *
         * @return the list of all invoices
         */
        public List<Invoice> getAll() {
            return invoices;
        }

        /**
         * Finds one invoice by ID. Upper and lower case do not matter.
         *
         * @param id the ID to look for
         * @return the matching invoice, or null if none has that ID
         */
        public Invoice findById(String id) {
            for (Invoice i : invoices) {
                if (i.getId().equalsIgnoreCase(id)) {
                    return i;
                }
            }
            return null;
        }

        /**
         * Removes an invoice by ID and saves the file.
         *
         * @param id the ID of the invoice to remove
         * @return true if it was removed, false if no invoice has that ID
         */
        public boolean remove(String id) {
            Invoice invoice = findById(id);
            if (invoice == null) {
                return false;
            }
            invoices.remove(invoice);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after changing an invoice amount.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the invoices, smallest amount first. The original list is not changed.
         *
         * @return a new list of invoices sorted by amount
         */
        public List<Invoice> sortedByAmount() {
            List<Invoice> copy = new ArrayList<>(invoices);
            // comparingDouble sorts by a decimal-number field, here the invoice amount.
            copy.sort(Comparator.comparingDouble(Invoice::getAmount));
            return copy;
        }
    }

    /**
     * BillingMenu is the screen the user sees for the Billing module. It shows the options,
     * reads the choice, and calls the right action.
     *
     * @author Ethar
     */
    public static class BillingMenu {
        // The invoice manager, plus the four other managers used to look up the records to bill.
        private BillingService service;
        private PatientService patientService;
        private ConsultationService consultationService;
        private PrescriptionService prescriptionService;
        private LabTestService labTestService;

        /**
         * Creates the menu.
         *
         * @param service the billing service that keeps the invoices
         * @param patientService the patient service, used to check patient IDs
         * @param consultationService the consultation service, used to look up visits to bill
         * @param prescriptionService the prescription service, used to look up prescriptions to bill
         * @param labTestService the lab test service, used to look up lab tests to bill
         */
        public BillingMenu(BillingService service, PatientService patientService, ConsultationService consultationService,
                           PrescriptionService prescriptionService, LabTestService labTestService) {
            this.service = service;
            this.patientService = patientService;
            this.consultationService = consultationService;
            this.prescriptionService = prescriptionService;
            this.labTestService = labTestService;
        }

        /**
         * Shows the Billing menu over and over, until the user picks 0.
         */
        public void show() {
            boolean running = true;
            while (running) {
                int choice = InputHelper.showMenu("BILLING", "Back to main menu",
                        "Create a new invoice",
                        "Show one invoice (with what it bills)",
                        "Change an invoice amount",
                        "Remove an invoice",
                        "Billing report (sorted by amount)");

                // Run the action that matches the number the user typed.
                switch (choice) {
                    case 1: createInvoice(); break;
                    case 2: showOne(); break;
                    case 3: changeAmount(); break;
                    case 4: remove(); break;
                    case 5: report(); break;
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
        // OPTION 1: Create a new invoice
        // ============================================================
        /**
         * Creates an invoice for a patient. The user can link a consultation, a prescription and a lab test,
         * or leave any of them blank to skip that link.
         */
        private void createInvoice() {
            String patientId = InputHelper.readNonEmptyText("Patient ID: ");
            Patient patient = patientService.findById(patientId);
            if (patient == null) {
                System.out.println("No patient found with that ID.");
                return;
            }
            // The list that will hold everything this invoice bills for. It starts empty.
            List<Record> billedItems = new ArrayList<>();

            // readText (not readNonEmptyText) is used here, because leaving the line blank is allowed.
            String consultationId = InputHelper.readText("Consultation ID to bill (leave blank to skip): ");
            if (!consultationId.isEmpty()) {
                Consultation consultation = consultationService.findById(consultationId);
                if (consultation != null) {
                    billedItems.add(consultation);
                } else {
                    System.out.println("No consultation found with that ID, skipping it.");
                }
            }

            String prescriptionId = InputHelper.readText("Prescription ID to bill (leave blank to skip): ");
            if (!prescriptionId.isEmpty()) {
                Prescription prescription = prescriptionService.findById(prescriptionId);
                if (prescription != null) {
                    billedItems.add(prescription);
                } else {
                    System.out.println("No prescription found with that ID, skipping it.");
                }
            }

            String labTestId = InputHelper.readText("Lab test ID to bill (leave blank to skip): ");
            if (!labTestId.isEmpty()) {
                LabTest labTest = labTestService.findById(labTestId);
                if (labTest != null) {
                    billedItems.add(labTest);
                } else {
                    System.out.println("No lab test found with that ID, skipping it.");
                }
            }

            double amount = InputHelper.readDouble("Total amount to charge: ");
            try {
                String id = service.generateId();
                service.add(new Invoice(id, patient, billedItems, amount));
                System.out.println("Invoice created with ID " + id);
                // Print a receipt line, for example "Total charge for Ali Test: 150.0".
                System.out.println("Receipt: " + new InvoiceItem("Total charge for " + patient.getName(), amount));
            } catch (InvalidPaymentException e) {
                System.out.println("Could not create invoice: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Show one invoice (with what it bills)
        // ============================================================
        /**
         * Shows one invoice and every record it bills. Each billed item (consultation, prescription or lab test)
         * is a Record, so the loop can call describe() on it without knowing its exact type.
         */
        private void showOne() {
            String id = InputHelper.readNonEmptyText("Enter invoice ID: ");
            Invoice invoice = service.findById(id);
            if (invoice == null) {
                System.out.println("No invoice found with that ID.");
                return;
            }
            System.out.println(invoice.describe());
            // isEmpty() is true when nothing is billed on this invoice.
            if (invoice.getBilledItems().isEmpty()) {
                System.out.println("(No linked services on this invoice.)");
            } else {
                System.out.println("Billed for:");
                // Print each billed item on its own indented line.
                for (Record item : invoice.getBilledItems()) {
                    System.out.println("  - " + item.describe());
                }
            }
        }

        // ============================================================
        // OPTION 3: Change an invoice amount
        // ============================================================
        /**
         * Finds an invoice by ID and changes its total amount.
         */
        private void changeAmount() {
            String id = InputHelper.readNonEmptyText("Enter invoice ID: ");
            Invoice invoice = service.findById(id);
            if (invoice == null) {
                System.out.println("No invoice found with that ID.");
                return;
            }
            try {
                // readDouble makes sure a real number is typed, and the setter rejects negative values.
                invoice.setAmount(InputHelper.readDouble("New total amount: "));
                service.update();
                System.out.println("Amount updated.");
            } catch (InvalidPaymentException e) {
                System.out.println("Could not update invoice: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 4: Remove an invoice
        // ============================================================
        /**
         * Removes an invoice by ID.
         */
        private void remove() {
            String id = InputHelper.readNonEmptyText("Enter invoice ID to remove: ");
            System.out.println(service.remove(id) ? "Invoice removed." : "No invoice found with that ID.");
        }

        // ============================================================
        // OPTION 5: Billing report (sorted by amount)
        // ============================================================
        /**
         * Prints every invoice, smallest amount first.
         */
        private void report() {
            List<Invoice> sorted = service.sortedByAmount();
            if (sorted.isEmpty()) {
                System.out.println("No invoices to report.");
                return;
            }
            System.out.println("--- Billing Report (sorted by amount) ---");
            // Each invoice is a Record, so describe() works on it.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }
    }
}
