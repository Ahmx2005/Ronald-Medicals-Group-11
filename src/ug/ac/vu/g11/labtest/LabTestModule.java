package ug.ac.vu.g11.labtest;

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
 * LAB TESTS module. Owner: Abrar.
 *
 * A lab test is a test sent to a laboratory for a patient, for example a malaria test. The test is
 * ordered first, with no result yet. The result is typed in later, once the laboratory reports it.
 * Each lab test gets an ID such as G11-LT001.
 *
 * The file contains five classes. Read them in this order:
 *   InvalidResultException - the error we throw when a test or result is wrong
 *   LabResult              - the result that was found (a "model" class: it just holds data)
 *   LabTest                - one test for one patient (a "model" class that extends Record and links to a real Patient)
 *   LabTestService         - keeps the list of tests, and adds, finds, updates and removes them
 *   LabTestMenu            - the screen the user sees for the Lab Tests module
 *
 * @author Abrar
 */
public class LabTestModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". Throwing it stops the action
     * and carries a message that explains the problem.
     *
     * @author Abrar
     */
    public static class InvalidResultException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user
         */
        public InvalidResultException(String message) {
            // "super" passes the message up to the Exception class, which stores it for later.
            super(message);
        }
    }

    /**
     * LabResult holds the outcome of one lab test, for example "Positive" or "Hb 11.2".
     * It is a "model class": it holds one piece of data and refuses an empty value.
     *
     * @author Abrar
     */
    public static class LabResult {
        // The result as text. Text is used so the lab can write things like "Positive" or "Hb 11.2".
        private String value;

        /**
         * Creates a result. The value must not be blank.
         *
         * @param value the result text, for example "Negative"
         * @throws InvalidResultException if the value is blank
         */
        public LabResult(String value) throws InvalidResultException {
            // The setter does the checking, so the same rule applies everywhere.
            setValue(value);
        }

        /**
         * Gives back the result text.
         *
         * @return the result, for example "Negative"
         */
        public String getValue() {
            return value;
        }

        /**
         * Changes the result, but only if it is not blank.
         *
         * @param value the new result text
         * @throws InvalidResultException if the value is null, empty or only spaces
         */
        public void setValue(String value) throws InvalidResultException {
            if (value == null || value.trim().isEmpty()) {
                throw new InvalidResultException("Result value cannot be empty.");
            }
            this.value = value;
        }
    }

    /**
     * One lab test ordered for a patient. It extends Record (so it has an ID and describe())
     * and implements Reportable (so it has getSummary()).
     *
     * The result starts out as null, which in Java means "there is no object here". A null result
     * means the test is still pending.
     *
     * @author Abrar
     */
    public static class LabTest extends Record implements Reportable {
        // The patient the test is for. An object reference, so the patient's details are always current.
        private Patient patient;
        // The name of the test, for example "Malaria test".
        private String testName;
        // The result, or null while the test is still pending.
        private LabResult result;

        /**
         * Creates a new lab test. The result can be null when the test is first ordered.
         *
         * @param id the unique ID, for example G11-LT001
         * @param patient the real patient the test is for
         * @param testName the name of the test
         * @param result the result, or null if it is not known yet
         * @throws InvalidResultException if the patient or test name is missing
         */
        public LabTest(String id, Patient patient, String testName, LabResult result) throws InvalidResultException {
            // Runs the Record constructor first, which sets the ID.
            super(id);
            setPatient(patient);
            setTestName(testName);
            // A null result is allowed here, so we store whatever we were given.
            this.result = result;
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
         * Links the test to a patient. A test cannot exist without a real patient.
         *
         * @param patient the patient to link
         * @throws InvalidResultException if the patient is null
         */
        public void setPatient(Patient patient) throws InvalidResultException {
            if (patient == null) {
                throw new InvalidResultException("A lab test must be linked to a real patient.");
            }
            this.patient = patient;
        }

        /**
         * Gives back the name of the test.
         *
         * @return the test name
         */
        public String getTestName() {
            return testName;
        }

        /**
         * Changes the test name, but only if it is not blank.
         *
         * @param testName the new name
         * @throws InvalidResultException if the name is null, empty or only spaces
         */
        public void setTestName(String testName) throws InvalidResultException {
            if (testName == null || testName.trim().isEmpty()) {
                throw new InvalidResultException("Test name cannot be empty.");
            }
            this.testName = testName;
        }

        /**
         * Records the result of the test. This is how a pending test becomes a finished one.
         *
         * @param result the result to store
         */
        public void setResult(LabResult result) {
            this.result = result;
        }

        /**
         * Turns the test into one line for the file. Only the patient's ID is saved.
         * If there is no result yet, the last part is empty, which is how a pending test is recognised later.
         * Example: G11-LT001|G11-PT001|Malaria test|Negative
         *
         * @return the one-line text form of this test
         */
        public String toLine() {
            // This is a "ternary" shorthand: "(condition) ? valueIfTrue : valueIfFalse".
            // If there is no result, we save an empty piece of text instead.
            String resultPart = (result == null) ? "" : result.getValue();
            return getId() + "|" + patient.getId() + "|" + testName + "|" + resultPart;
        }

        /**
         * Rebuilds a lab test from one line of the file. It finds the patient by ID.
         *
         * @param line one saved line from labtests.txt
         * @param patientService the patient list, used to find the patient
         * @return a new LabTest made from the line
         * @throws InvalidResultException if the patient no longer exists or a value is invalid
         */
        public static LabTest fromLine(String line, PatientService patientService) throws InvalidResultException {
            // Cut the line at each | into pieces. parts[3] is the result, which may be empty.
            String[] parts = line.split("\\|", -1);
            Patient patient = patientService.findById(parts[1]);
            if (patient == null) {
                throw new InvalidResultException("Patient for this lab test no longer exists.");
            }
            // An empty result part means the test is still pending, so the result stays null.
            LabResult result = parts[3].isEmpty() ? null : new LabResult(parts[3]);
            return new LabTest(parts[0], patient, parts[2], result);
        }

        /**
         * Describes the test in a full sentence. Required by the Record parent class.
         * Example: "Lab Test G11-LT001: Ali Test - Malaria test - result Negative".
         *
         * @return the sentence describing this test
         */
        @Override
        public String describe() {
            // A null result means the test is still waiting, so we say "result pending" instead.
            String resultText = (result == null) ? "result pending" : "result " + result.getValue();
            return "Lab Test " + getId() + ": " + patient.getName() + " - " + testName + " - " + resultText;
        }

        /**
         * Gives a short one-line summary, used in lists.
         *
         * @return a short summary, for example "G11-LT001 | Ali Test | Malaria test"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + patient.getName() + " | " + testName;
        }
    }

    /**
     * LabTestService is the "manager" of all lab tests. It keeps every test in an ArrayList,
     * loads the list from labtests.txt when the program starts, and saves after every change.
     *
     * @author Abrar
     */
    public static class LabTestService {
        // The file where tests are saved, and the text every lab test ID starts with.
        private static final String FILE_NAME = "labtests.txt";
        private static final String ID_PREFIX = "G11-LT";

        // The list of tests, the next ID number, and the patient list (needed when loading).
        private List<LabTest> labTests = new ArrayList<>();
        private int nextNumber = 1;
        private PatientService patientService;

        /**
         * Creates the service and loads the saved tests.
         *
         * @param patientService the patient service, used to link each test to its patient
         */
        public LabTestService(PatientService patientService) {
            this.patientService = patientService;
            load();
        }

        /**
         * Reads labtests.txt and rebuilds each test. A damaged line is skipped with a message.
         * Then it works out the next free ID number.
         */
        private void load() {
            for (String line : FileStorage.readLines(FILE_NAME)) {
                try {
                    labTests.add(LabTest.fromLine(line, patientService));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged lab test record in the file.");
                }
            }
            // Find the biggest number already used, so new IDs never clash with old ones.
            for (LabTest t : labTests) {
                try {
                    int number = Integer.parseInt(t.getId().replace(ID_PREFIX, ""));
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Writes every lab test to labtests.txt.
         */
        private void save() {
            List<String> lines = new ArrayList<>();
            for (LabTest t : labTests) {
                lines.add(t.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next lab test ID and moves the counter on.
         *
         * @return the new ID, for example G11-LT001
         */
        public String generateId() {
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            nextNumber++;
            return id;
        }

        /**
         * Adds a new lab test to the list and saves the file.
         *
         * @param labTest the test to add
         */
        public void add(LabTest labTest) {
            labTests.add(labTest);
            save();
        }

        /**
         * Gives back the whole list of lab tests.
         *
         * @return the list of all lab tests
         */
        public List<LabTest> getAll() {
            return labTests;
        }

        /**
         * Finds one lab test by ID. Upper and lower case do not matter.
         *
         * @param id the ID to look for
         * @return the matching test, or null if none has that ID
         */
        public LabTest findById(String id) {
            for (LabTest t : labTests) {
                if (t.getId().equalsIgnoreCase(id)) {
                    return t;
                }
            }
            return null;
        }

        /**
         * Removes a lab test by ID and saves the file.
         *
         * @param id the ID of the test to remove
         * @return true if it was removed, false if no test has that ID
         */
        public boolean remove(String id) {
            LabTest labTest = findById(id);
            if (labTest == null) {
                return false;
            }
            labTests.remove(labTest);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after entering a result.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the tests, from A to Z by test name. The original list is not changed.
         *
         * @return a new list of tests sorted by test name
         */
        public List<LabTest> sortedByTestName() {
            List<LabTest> copy = new ArrayList<>(labTests);
            // Comparator.comparing names the field to sort by: the test name.
            copy.sort(Comparator.comparing(LabTest::getTestName));
            return copy;
        }
    }

    /**
     * LabTestMenu is the screen the user sees for the Lab Tests module. It shows the options,
     * reads the choice, and calls the right action.
     *
     * @author Abrar
     */
    public static class LabTestMenu {
        // The lab test manager, and the patient manager (used to check that a patient exists).
        private LabTestService service;
        private PatientService patientService;

        /**
         * Creates the menu.
         *
         * @param service the lab test service that keeps the tests
         * @param patientService the patient service, used to check patient IDs
         */
        public LabTestMenu(LabTestService service, PatientService patientService) {
            this.service = service;
            this.patientService = patientService;
        }

        /**
         * Shows the Lab Tests menu over and over, until the user picks 0.
         */
        public void show() {
            boolean running = true;
            while (running) {
                int choice = InputHelper.showMenu("LAB TESTS", "Back to main menu",
                        "Order a new lab test",
                        "Enter a result for a test",
                        "Remove a lab test",
                        "Lab test report (sorted by test name)");

                // Run the action that matches the number the user typed.
                switch (choice) {
                    case 1: order(); break;
                    case 2: enterResult(); break;
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
        // OPTION 1: Order a new lab test
        // ============================================================
        /**
         * Orders a new test for a registered patient. The result is left empty until it is entered later.
         */
        private void order() {
            String patientId = InputHelper.readNonEmptyText("Patient ID: ");
            Patient patient = patientService.findById(patientId);
            if (patient == null) {
                System.out.println("No patient found with that ID.");
                return;
            }
            String testName = InputHelper.readNonEmptyText("Test name (e.g. Malaria test): ");
            try {
                // Passing null as the result means "not known yet".
                String id = service.generateId();
                service.add(new LabTest(id, patient, testName, null));
                System.out.println("Lab test ordered with ID " + id);
            } catch (InvalidResultException e) {
                System.out.println("Could not order lab test: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Enter a result for a test
        // ============================================================
        /**
         * Finds a test by ID and records the result the laboratory reported.
         */
        private void enterResult() {
            String id = InputHelper.readNonEmptyText("Enter lab test ID: ");
            LabTest test = service.findById(id);
            if (test == null) {
                System.out.println("No lab test found with that ID.");
                return;
            }
            try {
                // Build a checked LabResult from the typed text, store it on the test, then save.
                test.setResult(new LabResult(InputHelper.readNonEmptyText("Result: ")));
                service.update();
                System.out.println("Result recorded.");
            } catch (InvalidResultException e) {
                System.out.println("Could not record result: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 3: Remove a lab test
        // ============================================================
        /**
         * Removes a test by ID.
         */
        private void remove() {
            String id = InputHelper.readNonEmptyText("Enter lab test ID to remove: ");
            System.out.println(service.remove(id) ? "Lab test removed." : "No lab test found with that ID.");
        }

        // ============================================================
        // OPTION 4: Lab test report (sorted by test name)
        // ============================================================
        /**
         * Prints every test, sorted by test name.
         */
        private void report() {
            List<LabTest> sorted = service.sortedByTestName();
            if (sorted.isEmpty()) {
                System.out.println("No lab tests to report.");
                return;
            }
            System.out.println("--- Lab Test Report (sorted by test name) ---");
            // Each test is a Record, so describe() works on it.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }
    }
}
