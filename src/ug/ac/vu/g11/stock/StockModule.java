package ug.ac.vu.g11.stock;

import ug.ac.vu.g11.core.Core.FileStorage;
import ug.ac.vu.g11.core.Core.InputHelper;
import ug.ac.vu.g11.core.Core.Record;
import ug.ac.vu.g11.core.Core.Reportable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MEDICATION STOCK module. Owner: Rahaf.
 *
 * This module keeps track of how many units of each medicine the clinic has. Stock goes up when
 * a delivery arrives (restock) and goes down when the Prescription module gives medicine to a patient
 * (dispense). Each medicine gets an ID such as G11-MD001.
 *
 * The file contains five classes. Read them in this order:
 *   OutOfStockException - the error we throw when a stock detail is wrong or there is not enough stock
 *   StockMovement       - one stock change, shown as a short confirmation line (a "model" class)
 *   Medication          - one medicine and how many are in stock (a "model" class that extends Record)
 *   MedicationService   - keeps the list of medicines, and adds, finds, updates and removes them
 *   StockMenu           - the screen the user sees for the Medication Stock module
 *
 * @author Rahaf
 */
public class StockModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". This one is thrown for bad stock
     * details and when there are not enough units to give out.
     *
     * @author Rahaf
     */
    public static class OutOfStockException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user
         */
        public OutOfStockException(String message) {
            // "super" passes the message up to the Exception class, which stores it for later.
            super(message);
        }
    }

    /**
     * StockMovement describes one change to stock, such as a restock or a dispense. It is only used
     * to print a confirmation line, so it is not saved to the file.
     *
     * @author Rahaf
     */
    public static class StockMovement {
        // The medicine's name, the kind of change ("RESTOCK" or "DISPENSE"), and how many units.
        private String medicationName;
        private String type;
        private int amount;

        /**
         * Creates a stock movement. Each value is checked by its setter.
         *
         * @param medicationName the medicine's name, for example "Paracetamol"
         * @param type the kind of change, for example "RESTOCK"
         * @param amount how many units moved, more than zero
         * @throws OutOfStockException if any value is invalid
         */
        public StockMovement(String medicationName, String type, int amount) throws OutOfStockException {
            setMedicationName(medicationName);
            setType(type);
            setAmount(amount);
        }

        /**
         * Changes the medicine name, but only if it is not blank.
         *
         * @param medicationName the new name
         * @throws OutOfStockException if the name is null, empty or only spaces
         */
        public void setMedicationName(String medicationName) throws OutOfStockException {
            if (medicationName == null || medicationName.trim().isEmpty()) {
                throw new OutOfStockException("Medication name cannot be empty.");
            }
            this.medicationName = medicationName;
        }

        /**
         * Changes the movement type, but only if it is not blank.
         *
         * @param type the new type, for example "DISPENSE"
         * @throws OutOfStockException if the type is null, empty or only spaces
         */
        public void setType(String type) throws OutOfStockException {
            if (type == null || type.trim().isEmpty()) {
                throw new OutOfStockException("Movement type cannot be empty.");
            }
            this.type = type;
        }

        /**
         * Gives back the number of units that moved.
         *
         * @return the amount
         */
        public int getAmount() {
            return amount;
        }

        /**
         * Changes the amount, but only if it is more than zero. A stock change of zero means nothing happened.
         *
         * @param amount the new amount
         * @throws OutOfStockException if the amount is zero or below
         */
        public void setAmount(int amount) throws OutOfStockException {
            if (amount <= 0) {
                throw new OutOfStockException("Movement amount must be more than zero.");
            }
            this.amount = amount;
        }

        /**
         * Describes the movement in one line. The toString() name is special: Java uses it automatically
         * whenever an object is printed or joined into text.
         * Example: "DISPENSE 2 of Paracetamol".
         *
         * @return the one-line description
         */
        @Override
        public String toString() {
            return type + " " + amount + " of " + medicationName;
        }
    }

    /**
     * Medication is one medicine in stock, with the number of units left. It extends Record (so it has
     * an ID and describe()) and implements Reportable (so it has getSummary()).
     *
     * @author Rahaf
     */
    public static class Medication extends Record implements Reportable {
        // The medicine's name, for example "Paracetamol", and how many units are left.
        private String name;
        private int quantityInStock;

        /**
         * Creates a medicine record.
         *
         * @param id the unique ID, for example G11-MD001
         * @param name the medicine's name
         * @param quantityInStock how many units are in stock now, zero or more
         * @throws OutOfStockException if the name is blank or the quantity is negative
         */
        public Medication(String id, String name, int quantityInStock) throws OutOfStockException {
            // Runs the Record constructor first, which sets the ID.
            super(id);
            setName(name);
            setQuantityInStock(quantityInStock);
        }

        /**
         * Gives back the medicine's name.
         *
         * @return the name, for example "Paracetamol"
         */
        public String getName() {
            return name;
        }

        /**
         * Changes the medicine's name, but only if it is not blank.
         *
         * @param name the new name
         * @throws OutOfStockException if the name is null, empty or only spaces
         */
        public void setName(String name) throws OutOfStockException {
            if (name == null || name.trim().isEmpty()) {
                throw new OutOfStockException("Medication name cannot be empty.");
            }
            this.name = name;
        }

        /**
         * Gives back how many units are in stock.
         *
         * @return the number of units in stock
         */
        public int getQuantityInStock() {
            return quantityInStock;
        }

        /**
         * Sets the stock level directly, but only if it is zero or more. Stock can never be negative.
         *
         * @param quantityInStock the new stock level
         * @throws OutOfStockException if the value is negative
         */
        public void setQuantityInStock(int quantityInStock) throws OutOfStockException {
            if (quantityInStock < 0) {
                throw new OutOfStockException("Stock quantity cannot be negative.");
            }
            this.quantityInStock = quantityInStock;
        }

        /**
         * Takes units out of stock, for example when a prescription is issued. It refuses to go below zero,
         * so you cannot give out more medicine than the clinic has.
         *
         * @param amount how many units to take out
         * @throws OutOfStockException if there are fewer units in stock than the amount requested
         */
        public void dispense(int amount) throws OutOfStockException {
            // "amount > quantityInStock" means "we are asked for more than we have".
            if (amount > quantityInStock) {
                throw new OutOfStockException("Not enough " + name + " in stock. Only " + quantityInStock + " left.");
            }
            // "-=" is shorthand for "take this many away from the current value".
            quantityInStock -= amount;
        }

        /**
         * Adds units to stock, for example when a delivery arrives.
         *
         * @param amount how many units were delivered, more than zero
         * @throws OutOfStockException if the amount is zero or below
         */
        public void restock(int amount) throws OutOfStockException {
            if (amount <= 0) {
                throw new OutOfStockException("Restock amount must be more than zero.");
            }
            // "+=" is shorthand for "add this many to the current value".
            quantityInStock += amount;
        }

        /**
         * Turns the medicine into one line for the file. The fields are separated by "|".
         * Example: G11-MD001|Paracetamol|58
         *
         * @return the one-line text form of this medicine
         */
        public String toLine() {
            return getId() + "|" + name + "|" + quantityInStock;
        }

        /**
         * Rebuilds a medicine from one line of the file.
         *
         * @param line one saved line from medications.txt
         * @return a new Medication made from the line
         * @throws OutOfStockException if a value in the line is invalid
         */
        public static Medication fromLine(String line) throws OutOfStockException {
            String[] parts = line.split("\\|", -1);
            return new Medication(parts[0], parts[1], Integer.parseInt(parts[2]));
        }

        /**
         * Describes the medicine in a full sentence. Required by the Record parent class.
         * Example: "Medication G11-MD001: Paracetamol, 58 in stock".
         *
         * @return the sentence describing this medicine
         */
        @Override
        public String describe() {
            return "Medication " + getId() + ": " + name + ", " + quantityInStock + " in stock";
        }

        /**
         * Gives a short one-line summary, used in lists.
         *
         * @return a short summary, for example "G11-MD001 | Paracetamol | qty 58"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + name + " | qty " + quantityInStock;
        }
    }

    /**
     * MedicationService is the "manager" of all medicines. It keeps every medicine in an ArrayList,
     * loads the list from medications.txt when the program starts, and saves after every change.
     *
     * @author Rahaf
     */
    public static class MedicationService {
        // The file where medicines are saved, and the text every medication ID starts with.
        private static final String FILE_NAME = "medications.txt";
        private static final String ID_PREFIX = "G11-MD";

        // The list of medicines and the next ID number to use.
        private List<Medication> medications = new ArrayList<>();
        private int nextNumber = 1;

        /**
         * Creates the service and loads the saved medicines.
         */
        public MedicationService() {
            load();
        }

        /**
         * Reads medications.txt and rebuilds each medicine. A damaged line is skipped with a message.
         * Then it works out the next free ID number.
         */
        private void load() {
            for (String line : FileStorage.readLines(FILE_NAME)) {
                try {
                    medications.add(Medication.fromLine(line));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged medication record in the file.");
                }
            }
            // Find the biggest number already used, so new IDs never clash with old ones.
            for (Medication m : medications) {
                try {
                    int number = Integer.parseInt(m.getId().replace(ID_PREFIX, ""));
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Writes every medicine to medications.txt.
         */
        private void save() {
            List<String> lines = new ArrayList<>();
            for (Medication m : medications) {
                lines.add(m.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next medication ID and moves the counter on.
         *
         * @return the new ID, for example G11-MD001
         */
        public String generateId() {
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            nextNumber++;
            return id;
        }

        /**
         * Adds a new medicine to the list and saves the file.
         *
         * @param medication the medicine to add
         */
        public void add(Medication medication) {
            medications.add(medication);
            save();
        }

        /**
         * Gives back the whole list of medicines.
         *
         * @return the list of all medicines
         */
        public List<Medication> getAll() {
            return medications;
        }

        /**
         * Finds one medicine by ID. Upper and lower case do not matter.
         *
         * @param id the ID to look for
         * @return the matching medicine, or null if none has that ID
         */
        public Medication findById(String id) {
            for (Medication m : medications) {
                if (m.getId().equalsIgnoreCase(id)) {
                    return m;
                }
            }
            return null;
        }

        /**
         * Removes a medicine by ID and saves the file.
         *
         * @param id the ID of the medicine to remove
         * @return true if it was removed, false if no medicine has that ID
         */
        public boolean remove(String id) {
            Medication medication = findById(id);
            if (medication == null) {
                return false;
            }
            medications.remove(medication);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after any change to stock levels.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the medicines, lowest stock first, so the nearly-empty ones appear at the top.
         * The original list is not changed.
         *
         * @return a new list of medicines sorted by stock level
         */
        public List<Medication> sortedByStock() {
            List<Medication> copy = new ArrayList<>(medications);
            // comparingInt sorts by a whole-number field, here the quantity in stock.
            copy.sort(Comparator.comparingInt(Medication::getQuantityInStock));
            return copy;
        }
    }

    /**
     * StockMenu is the screen the user sees for the Medication Stock module. It shows the options,
     * reads the choice, and calls the right action.
     *
     * @author Rahaf
     */
    public static class StockMenu {
        // The medicine manager that does the real work.
        private MedicationService service;

        /**
         * Creates the menu.
         *
         * @param service the medication service that keeps the stock list
         */
        public StockMenu(MedicationService service) {
            this.service = service;
        }

        /**
         * Shows the Medication Stock menu over and over, until the user picks 0.
         */
        public void show() {
            boolean running = true;
            while (running) {
                int choice = InputHelper.showMenu("MEDICATION STOCK", "Back to main menu",
                        "Add a new medication",
                        "Restock a medication",
                        "Remove a medication",
                        "Stock report (sorted by quantity)");

                // Run the action that matches the number the user typed.
                switch (choice) {
                    case 1: addMedication(); break;
                    case 2: restock(); break;
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
        // OPTION 1: Add a new medication
        // ============================================================
        /**
         * Asks for a medicine's name and starting stock, then saves the new medicine.
         */
        private void addMedication() {
            String name = InputHelper.readNonEmptyText("Medication name: ");
            int quantity = InputHelper.readInt("Starting quantity: ");
            try {
                String id = service.generateId();
                service.add(new Medication(id, name, quantity));
                System.out.println("Medication added with ID " + id);
            } catch (OutOfStockException e) {
                System.out.println("Could not add medication: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Restock a medication
        // ============================================================
        /**
         * Adds stock to one medicine and prints a confirmation line for the change.
         */
        private void restock() {
            String id = InputHelper.readNonEmptyText("Enter medication ID: ");
            Medication medication = service.findById(id);
            if (medication == null) {
                System.out.println("No medication found with that ID.");
                return;
            }
            int amount = InputHelper.readInt("Amount to add: ");
            try {
                medication.restock(amount);
                service.update();
                // Print the confirmation line, for example "Recorded: RESTOCK 10 of Paracetamol".
                System.out.println("Recorded: " + new StockMovement(medication.getName(), "RESTOCK", amount));
                System.out.println("Stock updated. New quantity: " + medication.getQuantityInStock());
            } catch (OutOfStockException e) {
                System.out.println("Could not restock: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 3: Remove a medication
        // ============================================================
        /**
         * Removes a medicine by ID.
         */
        private void remove() {
            String id = InputHelper.readNonEmptyText("Enter medication ID to remove: ");
            System.out.println(service.remove(id) ? "Medication removed." : "No medication found with that ID.");
        }

        // ============================================================
        // OPTION 4: Stock report (sorted by quantity)
        // ============================================================
        /**
         * Prints every medicine, lowest stock first.
         */
        private void report() {
            List<Medication> sorted = service.sortedByStock();
            if (sorted.isEmpty()) {
                System.out.println("No medications to report.");
                return;
            }
            System.out.println("--- Stock Report (sorted by quantity) ---");
            // Each medicine is a Record, so describe() works on it.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }

    }
}
