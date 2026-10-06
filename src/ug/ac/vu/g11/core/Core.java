package ug.ac.vu.g11.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * CORE: the shared pieces that every module of the Ronald Medicals system uses.
 *
 * Think of this file as the "toolbox" that all the other modules borrow from.
 * It is owned by the integration leads, David and Ethar, because every module depends on it.
 *
 * It contains four things:
 *   Record      - a parent class (a "blueprint" that other classes copy) for every saved object.
 *                 Every saved object has an ID, such as G11-PT001 for a patient.
 *   Reportable  - an interface (a "promise" a class makes) meaning "I can print a one-line summary".
 *   InputHelper - reads what the user types on the keyboard safely, so bad typing never crashes the program.
 *   FileStorage - reads and writes text files, so a missing or damaged file never crashes the program.
 *
 * The four parts are written as "nested" classes, which means they live inside this one file
 * and are used from other files as Core.Record, Core.InputHelper and so on.
 *
 * @author David and Ethar
 */
public class Core {

    /**
     * Record is the parent ("abstract") class for everything we save: patients, appointments,
     * invoices and so on. It is called abstract because you cannot create a plain Record on its own.
     * You can only create a child of it, such as a Patient. Every child must follow the rules of Record.
     *
     * Real-life picture: a blank form that every type of file card must start with. Every card
     * has a number on it (the ID), and every card must explain itself in words (describe()).
     *
     * @author David and Ethar
     */
    public static abstract class Record {
        // The ID is the unique code of this record, such as G11-PT001. "private" means only
        // this class can change it directly, so nobody can accidentally break the ID from outside.
        private String id;

        /**
         * Builds a record with its ID. Every record must have an ID, so an empty one is rejected.
         *
         * @param id the unique code for this record, for example G11-PT001
         * @throws IllegalArgumentException if the ID is missing or only spaces
         */
        public Record(String id) {
            // id == null means no text was given at all. trim() removes spaces at the start and end,
            // and isEmpty() checks whether anything is left. Both cases are rejected.
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Record ID cannot be empty.");
            }
            // "this.id" means "the id belonging to this object", and "id" is the value that was passed in.
            this.id = id;
        }

        /**
         * Gives back the ID of this record.
         *
         * @return the ID, for example G11-PT001
         */
        public String getId() {
            return id;
        }

        // "abstract" means this method has no body here. Each child class (Patient, Invoice and so on)
        // must write its own describe() that says how that kind of record is described in plain words.
        /**
         * Describes this record in plain words. Each module fills this in for its own records.
         *
         * @return a short sentence describing the record
         */
        public abstract String describe();
    }

    /**
     * Reportable is an "interface". An interface is a list of methods that a class promises to have.
     * A class that says "implements Reportable" must write a getSummary() method.
     *
     * Real-life picture: a job requirement. "Must be able to drive" does not say how you drive,
     * only that you can. Here, "must be able to print a summary line" says nothing about the layout.
     *
     * Having one shared promise lets the program treat different kinds of records the same way.
     *
     * @author David and Ethar
     */
    public interface Reportable {
        /**
         * Returns a one-line summary of this object, for example "G11-PT001 | Ali | age 30".
         *
         * @return the one-line summary
         */
        String getSummary();
    }

    /**
     * InputHelper is the one place where the program reads what the user types on the keyboard.
     * Every prompt in the whole program goes through here.
     *
     * Why this matters: if the program read keyboard input directly, typing "abc" where a number
     * is expected would crash it. InputHelper checks the input, and asks again politely if it is wrong.
     *
     * All methods here are "static". That means you call them directly on the class, for example
     * InputHelper.readInt("Age: "), without first creating an InputHelper object.
     *
     * @author David and Ethar
     */
    public static class InputHelper {
        // A Scanner is Java's tool for reading text typed on the keyboard (System.in).
        // Only one Scanner should read the keyboard, so the whole program shares this single one.
        private static Scanner scanner = new Scanner(System.in);

        /**
         * Reads one full line typed by the user. If the input has ended (for example the user pressed
         * Ctrl+Z on Windows, or the input file ran out), the program says "Goodbye!" and closes cleanly,
         * instead of crashing with an error message.
         *
         * @return the line that was typed, exactly as typed
         */
        private static String nextLine() {
            try {
                // "try" means: attempt this. If it fails, Java jumps to the "catch" part below.
                return scanner.nextLine();
            } catch (NoSuchElementException e) {
                // This runs only when there is no more input at all.
                System.out.println();
                System.out.println("Goodbye!");
                // System.exit(0) closes the whole program. The 0 means "closed normally, no error".
                System.exit(0);
                // Java requires a return here, but System.exit has already closed the program.
                return null;
            }
        }

        /**
         * Asks the user for a whole number (for example 30, but not 12.5 or "thirty").
         * It keeps asking until the user types something that is a whole number.
         *
         * @param prompt the question shown to the user, for example "Patient age: "
         * @return the whole number the user typed
         */
        public static int readInt(String prompt) {
            // "while (true)" means "repeat forever". The only way out is the "return" inside it.
            while (true) {
                // print (not println) keeps the cursor on the same line, so the user types right after the prompt.
                System.out.print(prompt);
                try {
                    // Integer.parseInt turns text such as "30" into the number 30.
                    // trim() removes accidental spaces first, so " 30 " also works.
                    return Integer.parseInt(nextLine().trim());
                } catch (NumberFormatException e) {
                    // parseInt throws this error when the text is not a whole number (for example "abc").
                    // Catching it means we can show a friendly message and ask again, instead of crashing.
                    System.out.println("Please type a whole number.");
                }
            }
        }

        /**
         * Asks the user for a number that may have a decimal point, such as 37.5.
         * It rejects "not a number" values (NaN) and infinite values, because they make no sense
         * for things like a body temperature or a money amount.
         *
         * @param prompt the question shown to the user, for example "Temperature (Celsius): "
         * @return the number the user typed
         */
        public static double readDouble(String prompt) {
            // Keep asking until a valid number is typed.
            while (true) {
                System.out.print(prompt);
                try {
                    // Double.parseDouble turns text such as "37.5" into the decimal number 37.5.
                    double value = Double.parseDouble(nextLine().trim());
                    // NaN means "Not a Number" (for example, the result of 0 divided by 0).
                    // Infinite means a number that is too big to be useful. Both are rejected below.
                    if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                        return value;
                    }
                } catch (NumberFormatException ignored) {
                    // The text was not a number at all, such as "abc". We do nothing here and let the
                    // program fall through to the message below, which asks the user to try again.
                }
                System.out.println("Please type a number, for example 12.5");
            }
        }

        /**
         * Asks the user for text that must not be empty. It also refuses the characters | and ~,
         * because the program uses them to separate the fields inside a saved line.
         *
         * Example of why: if a name were "Ali|Bad", the saved line would be split into the wrong
         * fields when the program starts again, and the record would be lost.
         *
         * @param prompt the question shown to the user, for example "Patient name: "
         * @return the text the user typed, with spaces at the start and end removed
         */
        public static String readNonEmptyText(String prompt) {
            while (true) {
                System.out.print(prompt);
                // trim() removes spaces at the start and end of what was typed.
                String line = nextLine().trim();
                if (line.contains("|") || line.contains("~")) {
                    // contains() checks whether the text has that character anywhere inside it.
                    System.out.println("The | and ~ characters cannot be used, please try again.");
                } else if (!line.isEmpty()) {
                    // The text is valid, so we give it back and stop asking.
                    return line;
                } else {
                    // The user just pressed Enter with nothing typed.
                    System.out.println("This cannot be empty, please try again.");
                }
            }
        }

        /**
         * Reads text that is allowed to be empty. Used for optional fields where the user can
         * simply press Enter to skip. For example, leaving the consultation ID blank on an invoice.
         *
         * @param prompt the question shown to the user
         * @return the text typed (may be an empty string, which means "skipped")
         */
        public static String readText(String prompt) {
            System.out.print(prompt);
            return nextLine().trim();
        }

        /**
         * Draws a numbered menu on the screen and asks the user to pick a number.
         * Option 0 is always the way back out of the menu. The label for it is passed in
         * (backLabel), so it can say "Back to main menu" or "Exit", depending on where we are.
         *
         * Java's "String... options" means you can pass any number of text options, such as
         * showMenu("PATIENTS", "Back", "Register", "Remove"). Inside this method, options is a list.
         *
         * @param title the heading shown at the top of the menu, for example "PATIENTS"
         * @param backLabel the text for option 0, for example "Back to main menu"
         * @param options the choices shown as 1, 2, 3 and so on
         * @return the number the user picked
         */
        public static int showMenu(String title, String backLabel, String... options) {
            // Each println prints one line. Together they draw a neat box around the title.
            System.out.println();
            System.out.println("==================================================");
            System.out.println("  " + title);
            System.out.println("==================================================");
            // A "for" loop repeats the code inside it once for each option.
            // i starts at 0 and goes up by 1 until it reaches the number of options.
            for (int i = 0; i < options.length; i++) {
                // People count from 1, but Java lists start at 0, so we show (i + 1).
                System.out.println("  " + (i + 1) + ". " + options[i]);
            }
            System.out.println("  0. " + backLabel);
            System.out.println("--------------------------------------------------");
            int choice = readInt("Type a number and press Enter: ");
            // Line between the user's choice and whatever the choice prints below it.
            System.out.println();
            System.out.println("--------------------------------------------------");
            return choice;
        }

        /**
         * Pauses after an action, so the user can read the result before the menu appears again.
         * The program waits until the user presses Enter.
         */
        public static void pressEnter() {
            System.out.println();
            System.out.print("Press Enter to go back to the menu...");
            nextLine();
        }
    }

    /**
     * FileStorage reads and writes the text files that keep the data after the program closes.
     * Each file holds one record per line, and the fields on a line are separated by a symbol
     * such as | (for example G11-PT001|Ali|30|0700|Ken~0700111).
     *
     * Real-life picture: a filing cabinet. Each line is one card, and the file is the drawer.
     *
     * @author David and Ethar
     */
    public static class FileStorage {

        /**
         * Reads every non-empty line from a file and returns them as a list.
         * If the file does not exist yet (for example, on the very first run), it returns an empty
         * list instead of crashing. That is how the program handles a "missing file" safely.
         *
         * @param filename the name of the file, for example "patients.txt"
         * @return the lines in the file, in order (an empty list if the file is missing or cannot be read)
         */
        public static List<String> readLines(String filename) {
            // An ArrayList is a list that grows automatically as we add things to it.
            List<String> lines = new ArrayList<>();
            // Files.exists checks whether the file is there. If not, we stop early and return the empty list.
            if (!Files.exists(Paths.get(filename))) {
                return lines;
            }
            // "try (...)" opens the file and automatically closes it again when we are done,
            // even if something goes wrong. This prevents the file from being left locked.
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(filename))) {
                String line;
                // readLine() gives back the next line of text, or null when there are no more lines.
                while ((line = reader.readLine()) != null) {
                    // Blank lines are skipped, so an accidental empty line does not become a fake record.
                    if (!line.trim().isEmpty()) {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                // IOException means the file exists but could not be read (for example, it is locked
                // by another program). We warn the user and carry on with no saved data for this file.
                System.out.println("Could not read " + filename + ", starting with no saved data for it.");
            }
            return lines;
        }

        /**
         * Saves a list of lines to a file. It replaces everything that was in the file before,
         * so the list must contain every record that should be kept.
         *
         * @param filename the name of the file to write, for example "patients.txt"
         * @param lines the complete list of lines to save, one line per record
         */
        public static void writeLines(String filename, List<String> lines) {
            // BufferedWriter is the tool that writes text into the file.
            // The "try (...)" form makes sure the file is closed (and the text is saved) at the end.
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
                // The "for each" loop: take each line in turn and write it out.
                for (String line : lines) {
                    writer.write(line);
                    // newLine() moves to the next line in the file, so each record gets its own line.
                    writer.newLine();
                }
            } catch (IOException e) {
                // If the file could not be written (for example, it is open in Excel), we tell the user
                // that their latest change was NOT saved, so they know to try again.
                System.out.println("Could not save to " + filename + ". Your latest change was not saved.");
            }
        }
    }
}
