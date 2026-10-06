package ug.ac.vu.g11.patient;

import ug.ac.vu.g11.core.Core.FileStorage;
import ug.ac.vu.g11.core.Core.InputHelper;
import ug.ac.vu.g11.core.Core.Record;
import ug.ac.vu.g11.core.Core.Reportable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * PATIENTS module. Owner: Palet.
 *
 * This file keeps everything about patients in one place. A patient is a person the clinic
 * has registered. Each patient gets an ID such as G11-PT001 and an emergency contact.
 *
 * The file contains five classes. Read them in this order:
 *   InvalidPatientException - the error we throw when patient details are wrong (for example a blank name)
 *   EmergencyContact        - the person to call in an emergency (a "model" class: it just holds data)
 *   Patient                 - one registered patient (a "model" class that extends Record)
 *   PatientService          - keeps the list of patients, and adds, finds, updates and removes them
 *   PatientMenu             - the screen the user sees for the Patients module
 *
 * Beginner note: a "class" is a blueprint. When the program runs, it creates "objects" from the blueprint.
 * For example, the Patient class is the blueprint, and "Ali Test, age 30" is one object made from it.
 *
 * @author Palet
 */
public class PatientModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". Throwing an exception stops the
     * current action and tells the caller what went wrong, so bad data is never saved.
     *
     * This class extends Exception, which makes it a custom error type. Its message is the text
     * that explains the problem, for example "Patient age must be between 0 and 120."
     *
     * @author Palet
     */
    public static class InvalidPatientException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user, for example "Patient name cannot be empty."
         */
        public InvalidPatientException(String message) {
            // "super" passes the message up to the Exception class, which stores it so it can be shown later.
            super(message);
        }
    }

    /**
     * EmergencyContact holds the name and phone number of the person to call in an emergency.
     * It is a "model class": a simple class whose job is to hold data and protect that data.
     *
     * Real-life picture: the "In case of emergency" box on a hospital admission form.
     *
     * @author Palet
     */
    public static class EmergencyContact {
        // "private" means only code inside this class can touch these fields directly.
        // Everyone else must use the getter and setter methods below.
        private String name;
        private String phone;

        /**
         * Creates an emergency contact. Both the name and the phone must be filled in.
         *
         * @param name the contact's name, for example "Ken"
         * @param phone the contact's phone number, for example "0700111"
         * @throws InvalidPatientException if the name or the phone is blank
         */
        public EmergencyContact(String name, String phone) throws InvalidPatientException {
            // Instead of storing the values straight away, we send them through the setters.
            // The setters are where the checking happens, so the same checks run every time.
            setName(name);
            setPhone(phone);
        }

        /**
         * Gives back the contact's name.
         *
         * @return the name of the emergency contact
         */
        public String getName() {
            return name;
        }

        /**
         * Changes the name, but only if it is not blank.
         *
         * @param name the new name
         * @throws InvalidPatientException if the name is null, empty or only spaces
         */
        public void setName(String name) throws InvalidPatientException {
            // "null" means no value at all. trim().isEmpty() catches a value that is only spaces.
            if (name == null || name.trim().isEmpty()) {
                throw new InvalidPatientException("Emergency contact name cannot be empty.");
            }
            // "this.name" is the field of this object. "name" on its own is the value we were given.
            this.name = name;
        }

        /**
         * Gives back the contact's phone number.
         *
         * @return the phone number
         */
        public String getPhone() {
            return phone;
        }

        /**
         * Changes the phone number, but only if it is not blank.
         *
         * @param phone the new phone number
         * @throws InvalidPatientException if the phone is null, empty or only spaces
         */
        public void setPhone(String phone) throws InvalidPatientException {
            if (phone == null || phone.trim().isEmpty()) {
                throw new InvalidPatientException("Emergency contact phone cannot be empty.");
            }
            this.phone = phone;
        }

        /**
         * Turns the contact into text for saving in the file. The "~" symbol separates the name from the phone.
         * For example, Ken and 0700111 become "Ken~0700111".
         *
         * @return the text form of this contact
         */
        public String toLine() {
            return name + "~" + phone;
        }

        /**
         * Rebuilds an emergency contact from text read from the file. This is the reverse of toLine().
         *
         * "static" means you call this on the class itself, for example EmergencyContact.fromLine(...),
         * because there is no contact object yet when we are building one from the text.
         *
         * @param text the saved text, for example "Ken~0700111"
         * @return a new EmergencyContact made from the text
         * @throws InvalidPatientException if the text is damaged or a part is blank
         */
        public static EmergencyContact fromLine(String text) throws InvalidPatientException {
            // split("~") cuts the text at each ~. The -1 keeps empty pieces at the end, so nothing is lost.
            // For "Ken~0700111" the parts are ["Ken", "0700111"]. parts[0] is the first piece.
            String[] parts = text.split("~", -1);
            return new EmergencyContact(parts[0], parts[1]);
        }
    }

    /**
     * One registered patient. This is a "model class" that extends Record, so every patient
     * automatically has an ID and the describe() method that the shared rules require.
     *
     * "extends Record" means Patient is a child of Record. "implements Reportable" means Patient
     * promises to have a getSummary() method, so it can be printed as a short line.
     * Use made-up names and phone numbers for testing.
     *
     * @author Palet
     */
    public static class Patient extends Record implements Reportable {
        // Each field stores one piece of information about the patient.
        private String name;
        private int age;
        private String phone;
        // This field holds a whole EmergencyContact object, not just text. This is an "object reference":
        // the patient "owns" a contact, so the two classes are linked.
        private EmergencyContact emergencyContact;

        /**
         * Creates a new patient. Every value is checked by its setter, so a bad value causes an error.
         *
         * @param id the patient's unique ID, for example G11-PT001
         * @param name the patient's full name
         * @param age the patient's age in years, from 0 to 120
         * @param phone the patient's phone number
         * @param emergencyContact the person to call in an emergency
         * @throws InvalidPatientException if any value is invalid
         */
        public Patient(String id, String name, int age, String phone, EmergencyContact emergencyContact)
                throws InvalidPatientException {
            // "super(id)" runs the constructor of the parent class (Record) first, and gives it the ID.
            super(id);
            setName(name);
            setAge(age);
            setPhone(phone);
            // The emergency contact is already a checked object, so we can store it directly.
            this.emergencyContact = emergencyContact;
        }

        /**
         * Gives back the patient's name.
         *
         * @return the name of the patient
         */
        public String getName() {
            return name;
        }

        /**
         * Changes the name, but only if it is not blank.
         *
         * @param name the new name
         * @throws InvalidPatientException if the name is null, empty or only spaces
         */
        public void setName(String name) throws InvalidPatientException {
            if (name == null || name.trim().isEmpty()) {
                throw new InvalidPatientException("Patient name cannot be empty.");
            }
            this.name = name;
        }

        /**
         * Changes the age, but only if it is between 0 and 120. Anything else is impossible for a person,
         * so it is rejected.
         *
         * @param age the new age in years
         * @throws InvalidPatientException if the age is below 0 or above 120
         */
        public void setAge(int age) throws InvalidPatientException {
            // "||" means "or". The age is rejected if it is less than 0 OR greater than 120.
            if (age < 0 || age > 120) {
                throw new InvalidPatientException("Patient age must be between 0 and 120.");
            }
            this.age = age;
        }

        /**
         * Gives back the patient's phone number.
         *
         * @return the phone number
         */
        public String getPhone() {
            return phone;
        }

        /**
         * Changes the phone number, but only if it is not blank.
         *
         * @param phone the new phone number
         * @throws InvalidPatientException if the phone is null, empty or only spaces
         */
        public void setPhone(String phone) throws InvalidPatientException {
            if (phone == null || phone.trim().isEmpty()) {
                throw new InvalidPatientException("Patient phone cannot be empty.");
            }
            this.phone = phone;
        }

        /**
         * Turns the patient into one line of text for the file. The fields are separated by "|".
         * Example: G11-PT001|Ali Test|30|0700|Ken~0700111
         *
         * @return the one-line text form of this patient
         */
        public String toLine() {
            // The "+" symbol joins text pieces together. getId() gets the ID from the Record parent class.
            return getId() + "|" + name + "|" + age + "|" + phone + "|" + emergencyContact.toLine();
        }

        /**
         * Rebuilds a patient from one line of the file. This is the reverse of toLine().
         *
         * @param line one saved line, for example G11-PT001|Ali Test|30|0700|Ken~0700111
         * @return a new Patient made from the line
         * @throws InvalidPatientException if a value in the line is invalid
         */
        public static Patient fromLine(String line) throws InvalidPatientException {
            // split("\\|") cuts the line at each | character. The two backslashes are needed because
            // | has a special meaning in this kind of pattern, so we "escape" it to mean a plain |.
            String[] parts = line.split("\\|", -1);
            // Integer.parseInt turns the text "30" into the number 30.
            // The emergency contact is the last piece, so it is rebuilt with its own fromLine().
            return new Patient(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3],
                    EmergencyContact.fromLine(parts[4]));
        }

        /**
         * Describes the patient in a full sentence. This is the method the Record parent class requires.
         * Example: "Patient G11-PT001: Ali Test, age 30, phone 0700".
         *
         * @return the sentence describing this patient
         */
        @Override
        public String describe() {
            return "Patient " + getId() + ": " + name + ", age " + age + ", phone " + phone;
        }

        /**
         * Gives a short one-line summary, used in lists. The @Override tag tells Java that this method
         * replaces the one promised by the Reportable interface.
         *
         * @return a short summary, for example "G11-PT001 | Ali Test | age 30"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + name + " | age " + age;
        }
    }

    /**
     * PatientService is the "manager" of all patients. It keeps every patient in an ArrayList
     * (a list that grows as needed), loads the list from patients.txt when the program starts,
     * and saves it again after every change.
     *
     * Real-life picture: the receptionist who keeps the patient file cabinet.
     *
     * @author Palet
     */
    public static class PatientService {
        // "static final" means the value is shared by the whole class and can never change.
        // The name of the file where patients are saved.
        private static final String FILE_NAME = "patients.txt";
        // Every patient ID starts with this text, so patient IDs look like G11-PT001.
        private static final String ID_PREFIX = "G11-PT";

        // The list of all patients currently in the program. "new ArrayList<>()" creates an empty list.
        private List<Patient> patients = new ArrayList<>();
        // The number to use for the next new patient. It starts at 1 and goes up.
        private int nextNumber = 1;

        /**
         * Creates the service and loads the saved patients. The program creates this once, at startup.
         */
        public PatientService() {
            load();
        }

        /**
         * Reads patients.txt and rebuilds each patient. A damaged line is skipped with a message,
         * so one bad line never stops the rest from loading.
         * It then works out the next free ID number by looking at the highest number already used.
         */
        private void load() {
            // The "for each" loop goes through every line the file gives us, one at a time.
            for (String line : FileStorage.readLines(FILE_NAME)) {
                // "try" tries to rebuild the patient. If anything goes wrong, "catch" runs instead.
                try {
                    patients.add(Patient.fromLine(line));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged patient record in the file.");
                }
            }
            // This second loop finds the biggest ID number already in use.
            for (Patient p : patients) {
                try {
                    // For "G11-PT012", replace("G11-PT", "") leaves "012", and parseInt turns it into 12.
                    int number = Integer.parseInt(p.getId().replace(ID_PREFIX, ""));
                    // If this number is bigger than what we have, the next free number is one more than it.
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Writes every patient to patients.txt. It is called after every change, so the file is always up to date.
         */
        private void save() {
            // Build a list of text lines, one line for each patient.
            List<String> lines = new ArrayList<>();
            for (Patient p : patients) {
                lines.add(p.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next patient ID and moves the counter on, so the next call gives a new number.
         *
         * @return the new ID, for example G11-PT001. "%03d" pads the number with zeros to 3 digits.
         */
        public String generateId() {
            // String.format builds the text. %s is the prefix and %03d is the number padded to 3 digits.
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            // The "++" symbol adds one, so the next patient gets the following number.
            nextNumber++;
            return id;
        }

        /**
         * Adds a new patient to the list and saves the file straight away.
         *
         * @param patient the patient to add
         */
        public void add(Patient patient) {
            patients.add(patient);
            save();
        }

        /**
         * Gives back the whole list of patients. The menus use this to check and read patients.
         *
         * @return the list of all patients
         */
        public List<Patient> getAll() {
            return patients;
        }

        /**
         * Finds one patient by ID. Upper and lower case do not matter, so "g11-pt001" also works.
         *
         * @param id the ID to look for
         * @return the matching patient, or null if no patient has that ID
         */
        public Patient findById(String id) {
            // Check each patient in turn, and return the first one whose ID matches.
            for (Patient p : patients) {
                // equalsIgnoreCase compares two pieces of text without caring about capital letters.
                if (p.getId().equalsIgnoreCase(id)) {
                    return p;
                }
            }
            // "null" is Java's way of saying "nothing here". We reach this only if no match was found.
            return null;
        }

        /**
         * Removes a patient by ID and saves the file.
         *
         * @param id the ID of the patient to remove
         * @return true if a patient was removed, false if no patient has that ID
         */
        public boolean remove(String id) {
            Patient patient = findById(id);
            if (patient == null) {
                return false;
            }
            patients.remove(patient);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after changing a patient (for example a new phone number),
         * so the change is written to the file and not lost when the program closes.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the patients, from A to Z by name. The original list is not changed.
         *
         * @return a new list of patients sorted by name
         */
        public List<Patient> sortedByName() {
            // Copy the list first, so sorting the copy does not reorder the real list.
            List<Patient> copy = new ArrayList<>(patients);
            // Comparator.comparing tells the sort which field to compare: the patient's name.
            // Patient::getName is a shortcut that means "for each patient, use its getName() result".
            copy.sort(Comparator.comparing(Patient::getName));
            return copy;
        }
    }

    /**
     * PatientMenu is the screen the user sees for the Patients module. It shows the options,
     * reads the user's choice, and calls the right action.
     *
     * Real-life picture: the reception desk's list of things the receptionist can do.
     *
     * @author Palet
     */
    public static class PatientMenu {
        // The service that does the real work, and the rule that checks if a patient has linked records.
        private PatientService service;
        private Predicate<String> hasLinkedRecords;

        /**
         * Creates the menu.
         *
         * @param service the patient service that keeps the patient list
         * @param hasLinkedRecords a rule that takes a patient ID and answers true if that patient still has
         *                         appointments, consultations, prescriptions, lab tests or invoices
         */
        public PatientMenu(PatientService service, Predicate<String> hasLinkedRecords) {
            this.service = service;
            this.hasLinkedRecords = hasLinkedRecords;
        }

        /**
         * Shows the Patients menu over and over, until the user picks 0 (Back to main menu).
         */
        public void show() {
            // "running" stays true while the menu should keep showing.
            boolean running = true;
            while (running) {
                // Draw the menu and read the number the user types.
                int choice = InputHelper.showMenu("PATIENTS", "Back to main menu",
                        "Register a new patient",
                        "Change a patient's phone number",
                        "Remove a patient",
                        "Patient report (sorted by name)");

                // Run the action that matches the number.
                switch (choice) {
                    case 1: register(); break;
                    case 2: changePhone(); break;
                    case 3: remove(); break;
                    case 4: report(); break;
                    case 0: running = false; break;
                    default: System.out.println("Please choose a number from the menu.");
                }
                // After an action (but not after going back), wait for Enter so the user can read the result.
                if (running) {
                    InputHelper.pressEnter();
                }
            }
        }

        // ============================================================
        // OPTION 1: Register a new patient
        // ============================================================
        /**
         * Asks the user for the patient's details, checks them, and saves the new patient.
         * If any detail is wrong, the error message is shown and nothing is saved.
         */
        private void register() {
            // Each line asks one question and stores the answer in a variable.
            String name = InputHelper.readNonEmptyText("Patient name: ");
            int age = InputHelper.readInt("Patient age: ");
            String phone = InputHelper.readNonEmptyText("Phone number: ");
            String contactName = InputHelper.readNonEmptyText("Emergency contact name: ");
            String contactPhone = InputHelper.readNonEmptyText("Emergency contact phone: ");
            // "try" lets us catch the error if any value is rejected by a setter.
            try {
                EmergencyContact contact = new EmergencyContact(contactName, contactPhone);
                // Get the next free ID, for example G11-PT004.
                String id = service.generateId();
                // Build the patient and save it.
                service.add(new Patient(id, name, age, phone, contact));
                System.out.println("Patient registered with ID " + id);
            } catch (InvalidPatientException e) {
                // getMessage() returns the explanation we wrote in the setter, for example "Patient age must be...".
                System.out.println("Could not register patient: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Change a patient's phone number
        // ============================================================
        /**
         * Finds a patient by ID and changes their phone number.
         */
        private void changePhone() {
            String id = InputHelper.readNonEmptyText("Enter patient ID: ");
            Patient patient = service.findById(id);
            // If the ID does not exist, say so and stop here. "return" leaves the method early.
            if (patient == null) {
                System.out.println("No patient found with that ID.");
                return;
            }
            try {
                // Ask for the new number, change it on the patient, then save the file.
                patient.setPhone(InputHelper.readNonEmptyText("New phone number: "));
                service.update();
                System.out.println("Phone number updated.");
            } catch (InvalidPatientException e) {
                System.out.println("Could not update: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 3: Remove a patient
        // ============================================================
        /**
         * Removes a patient, but only if they have no linked records. This protects the data:
         * if a patient were removed while they still had appointments, those appointments would
         * be lost the next time the program starts.
         */
        private void remove() {
            String id = InputHelper.readNonEmptyText("Enter patient ID to remove: ");
            // Ask the rule first. If it says "yes, linked records exist", refuse and stop.
            if (hasLinkedRecords.test(id)) {
                // The "+" joins two pieces of text, so this long message can sit on two lines in the code.
                System.out.println("Cannot remove this patient while they have appointments, consultations, "
                        + "prescriptions, lab tests or invoices. Remove those first.");
                return;
            }
            // The "? :" shorthand means: if the remove worked, show the first message; otherwise show the second.
            System.out.println(service.remove(id) ? "Patient removed." : "No patient found with that ID.");
        }

        // ============================================================
        // OPTION 4: Patient report (sorted by name)
        // ============================================================
        /**
         * Prints every patient, sorted by name. The loop treats each patient as a Record, so it can
         * call describe() on each one without caring exactly what kind of object it is.
         */
        private void report() {
            List<Patient> sorted = service.sortedByName();
            // isEmpty() is true when the list has no items, so we tell the user and stop.
            if (sorted.isEmpty()) {
                System.out.println("No patients to report.");
                return;
            }
            System.out.println("--- Patient Report (sorted by name) ---");
            // Print one line per patient.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }
    }
}
