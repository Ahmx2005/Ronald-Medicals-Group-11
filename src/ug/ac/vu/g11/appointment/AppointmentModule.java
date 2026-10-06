package ug.ac.vu.g11.appointment;

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
 * APPOINTMENT BOOKING module. Owner: Patcho.
 *
 * This file keeps everything about appointments in one place. An appointment is a booking that
 * links one patient to one doctor at one date and time. Each appointment gets an ID such as G11-AP001.
 *
 * The file contains five classes. Read them in this order:
 *   DoubleBookingException - the error we throw when a booking is invalid or the doctor is already busy
 *   Doctor                 - the doctor for an appointment (a "model" class: it just holds data)
 *   Appointment            - one booking (a "model" class that extends Record and links to a real Patient)
 *   AppointmentService     - keeps the list of bookings, and adds, finds, updates and removes them
 *   AppointmentMenu        - the screen the user sees for the Appointment Booking module
 *
 * Beginner note: a "double booking" means the same doctor is given two patients at the same time.
 * The program stops this from happening.
 *
 * @author Patcho
 */
public class AppointmentModule {

    /**
     * An "exception" is Java's way of saying "something went wrong". Throwing it stops the action
     * and carries a message that explains the problem. It is used both for invalid details and for
     * a double booking.
     *
     * @author Patcho
     */
    public static class DoubleBookingException extends Exception {
        /**
         * Creates the error with a message that explains the problem.
         *
         * @param message the explanation shown to the user
         */
        public DoubleBookingException(String message) {
            // "super" passes the message up to the Exception class, which stores it for later.
            super(message);
        }
    }

    /**
     * Doctor holds the name of the doctor who will see the patient. It is a "model class":
     * a small class whose job is to hold data and check that the data is valid.
     *
     * @author Patcho
     */
    public static class Doctor {
        // "private" means only this class can change the name directly. Other code must use setName().
        private String name;

        /**
         * Creates a doctor. The name must not be blank.
         *
         * @param name the doctor's name, for example "Dr Who"
         * @throws DoubleBookingException if the name is blank
         */
        public Doctor(String name) throws DoubleBookingException {
            // Sending the value through the setter means the same check runs for the constructor and for changes.
            setName(name);
        }

        /**
         * Gives back the doctor's name.
         *
         * @return the name of the doctor
         */
        public String getName() {
            return name;
        }

        /**
         * Changes the doctor's name, but only if it is not blank.
         *
         * @param name the new name
         * @throws DoubleBookingException if the name is null, empty or only spaces
         */
        public void setName(String name) throws DoubleBookingException {
            if (name == null || name.trim().isEmpty()) {
                throw new DoubleBookingException("Doctor name cannot be empty.");
            }
            this.name = name;
        }
    }

    /**
     * One booking between a patient and a doctor. It extends Record (so it has an ID and describe())
     * and implements Reportable (so it has getSummary()).
     *
     * The appointment does not copy the patient's details. It holds a reference to the real Patient
     * object, so the patient's name is always the up-to-date name. This is the "object reference"
     * that links two modules together.
     *
     * @author Patcho
     */
    public static class Appointment extends Record implements Reportable {
        // A Patient object, not just the patient's ID. Java looks the patient up by reference.
        private Patient patient;
        // The Doctor object for this booking.
        private Doctor doctor;
        // The date and time are stored as text, for example "2026-10-05" and "14:30".
        private String date;
        private String time;

        /**
         * Creates a new appointment. Each value is checked by its setter.
         *
         * @param id the unique ID, for example G11-AP001
         * @param patient the real patient this booking is for
         * @param doctor the doctor who will see the patient
         * @param date the date, for example 2026-10-05
         * @param time the time, for example 14:30
         * @throws DoubleBookingException if any value is missing
         */
        public Appointment(String id, Patient patient, Doctor doctor, String date, String time)
                throws DoubleBookingException {
            // Runs the Record constructor first, which sets the ID.
            super(id);
            setPatient(patient);
            // The doctor is already a checked Doctor object, so it can be stored directly.
            this.doctor = doctor;
            setDate(date);
            setTime(time);
        }

        /**
         * Gives back the patient this appointment is for.
         *
         * @return the linked Patient object
         */
        public Patient getPatient() {
            return patient;
        }

        /**
         * Links the appointment to a patient. A booking cannot exist without a real patient.
         *
         * @param patient the patient to link
         * @throws DoubleBookingException if the patient is null (no patient given)
         */
        public void setPatient(Patient patient) throws DoubleBookingException {
            if (patient == null) {
                throw new DoubleBookingException("An appointment must be linked to a real patient.");
            }
            this.patient = patient;
        }

        /**
         * Gives back the doctor for this appointment.
         *
         * @return the Doctor object
         */
        public Doctor getDoctor() {
            return doctor;
        }

        /**
         * Gives back the date of the appointment.
         *
         * @return the date as text, for example 2026-10-05
         */
        public String getDate() {
            return date;
        }

        /**
         * Changes the date, but only if it is not blank.
         *
         * @param date the new date
         * @throws DoubleBookingException if the date is null, empty or only spaces
         */
        public void setDate(String date) throws DoubleBookingException {
            if (date == null || date.trim().isEmpty()) {
                throw new DoubleBookingException("Appointment date cannot be empty.");
            }
            this.date = date;
        }

        /**
         * Gives back the time of the appointment.
         *
         * @return the time as text, for example 14:30
         */
        public String getTime() {
            return time;
        }

        /**
         * Changes the time, but only if it is not blank.
         *
         * @param time the new time
         * @throws DoubleBookingException if the time is null, empty or only spaces
         */
        public void setTime(String time) throws DoubleBookingException {
            if (time == null || time.trim().isEmpty()) {
                throw new DoubleBookingException("Appointment time cannot be empty.");
            }
            this.time = time;
        }

        /**
         * Turns the appointment into one line for the file. Only the patient's ID is saved,
         * not the whole patient. The fields are separated by "|".
         * Example: G11-AP001|G11-PT001|Dr Who|2026-10-07|14:30
         *
         * @return the one-line text form of this appointment
         */
        public String toLine() {
            return getId() + "|" + patient.getId() + "|" + doctor.getName() + "|" + date + "|" + time;
        }

        /**
         * Rebuilds an appointment from one line of the file. It looks up the real patient by ID,
         * because the file only stores the ID.
         *
         * @param line one saved line from appointments.txt
         * @param patientService the patient list, used to find the patient by ID
         * @return a new Appointment made from the line
         * @throws DoubleBookingException if the patient no longer exists or a value is missing
         */
        public static Appointment fromLine(String line, PatientService patientService) throws DoubleBookingException {
            // Cut the line into its pieces at each |. parts[0] is the ID, parts[1] the patient ID, and so on.
            String[] parts = line.split("\\|", -1);
            Patient patient = patientService.findById(parts[1]);
            // If the patient is missing, the booking cannot be rebuilt, so we refuse it.
            if (patient == null) {
                throw new DoubleBookingException("Patient for this appointment no longer exists.");
            }
            return new Appointment(parts[0], patient, new Doctor(parts[2]), parts[3], parts[4]);
        }

        /**
         * Describes the appointment in a full sentence. Required by the Record parent class.
         * Example: "Appointment G11-AP001: Ali Test with Dr. Dr Who on 2026-10-07 at 14:30".
         *
         * @return the sentence describing this appointment
         */
        @Override
        public String describe() {
            return "Appointment " + getId() + ": " + patient.getName() + " with Dr. " + doctor.getName()
                    + " on " + date + " at " + time;
        }

        /**
         * Gives a short one-line summary, used in lists.
         *
         * @return a short summary, for example "G11-AP001 | Ali Test | 2026-10-07 14:30"
         */
        @Override
        public String getSummary() {
            return getId() + " | " + patient.getName() + " | " + date + " " + time;
        }
    }

    /**
     * AppointmentService is the "manager" of all appointments. It keeps every booking in an ArrayList,
     * loads the list from appointments.txt when the program starts, and saves after every change.
     *
     * Real-life picture: the appointment book on the clinic reception desk.
     *
     * @author Patcho
     */
    public static class AppointmentService {
        // The file where bookings are saved, and the text every appointment ID starts with.
        private static final String FILE_NAME = "appointments.txt";
        private static final String ID_PREFIX = "G11-AP";

        // The list of all bookings in the program, and the next ID number to use.
        private List<Appointment> appointments = new ArrayList<>();
        private int nextNumber = 1;
        // The patient list. We need it to find the real patient for each booking when loading.
        private PatientService patientService;

        /**
         * Creates the service and loads the saved appointments.
         *
         * @param patientService the patient service, needed to link each booking to its patient
         */
        public AppointmentService(PatientService patientService) {
            this.patientService = patientService;
            load();
        }

        /**
         * Reads appointments.txt and rebuilds each booking. A damaged line, or one whose patient is missing,
         * is skipped with a message. Then it works out the next free ID number.
         */
        private void load() {
            for (String line : FileStorage.readLines(FILE_NAME)) {
                try {
                    appointments.add(Appointment.fromLine(line, patientService));
                } catch (Exception e) {
                    System.out.println("Skipped one damaged appointment record in the file.");
                }
            }
            // Find the biggest number already used, so new IDs never clash with old ones.
            for (Appointment a : appointments) {
                try {
                    // "G11-AP012" becomes "012", which becomes the number 12.
                    int number = Integer.parseInt(a.getId().replace(ID_PREFIX, ""));
                    if (number >= nextNumber) {
                        nextNumber = number + 1;
                    }
                } catch (NumberFormatException ignored) {
                    // An ID we cannot read is not counted.
                }
            }
        }

        /**
         * Writes every appointment to appointments.txt.
         */
        private void save() {
            List<String> lines = new ArrayList<>();
            for (Appointment a : appointments) {
                lines.add(a.toLine());
            }
            FileStorage.writeLines(FILE_NAME, lines);
        }

        /**
         * Creates the next appointment ID and moves the counter on.
         *
         * @return the new ID, for example G11-AP001
         */
        public String generateId() {
            String id = String.format("%s%03d", ID_PREFIX, nextNumber);
            nextNumber++;
            return id;
        }

        /**
         * Checks whether a doctor is already busy at a date and time. This is the simple version
         * used when booking a new appointment, where no existing booking should be ignored.
         *
         * @param doctorName the doctor to check
         * @param date the date to check
         * @param time the time to check
         * @return true if the doctor already has a booking at that date and time
         */
        public boolean isDoubleBooked(String doctorName, String date, String time) {
            // "null" here means "do not ignore any booking", so every existing booking is checked.
            return isDoubleBooked(doctorName, date, time, null);
        }

        /**
         * Checks whether a doctor is already busy at a date and time, but skips one booking.
         * This is needed when rescheduling: the booking being moved must not count as a clash with itself.
         *
         * @param doctorName the doctor to check
         * @param date the date to check
         * @param time the time to check
         * @param ignoreId the ID of a booking to skip, or null to skip none
         * @return true if a different booking already uses this doctor at that date and time
         */
        public boolean isDoubleBooked(String doctorName, String date, String time, String ignoreId) {
            // Check every booking in turn. The "&&" means "and": all the conditions must be true for a clash.
            for (Appointment a : appointments) {
                // "!" means "not". So "!a.getId().equals(ignoreId)" means "this is not the booking we are ignoring".
                if (!a.getId().equals(ignoreId)
                        && a.getDoctor().getName().equalsIgnoreCase(doctorName)
                        && a.getDate().equals(date)
                        && a.getTime().equals(time)) {
                    // A clash was found, so there is no need to look any further.
                    return true;
                }
            }
            return false;
        }

        /**
         * Adds a new appointment to the list and saves the file.
         *
         * @param appointment the appointment to add
         */
        public void add(Appointment appointment) {
            appointments.add(appointment);
            save();
        }

        /**
         * Gives back the whole list of appointments.
         *
         * @return the list of all appointments
         */
        public List<Appointment> getAll() {
            return appointments;
        }

        /**
         * Finds one appointment by ID. Upper and lower case do not matter.
         *
         * @param id the ID to look for
         * @return the matching appointment, or null if none has that ID
         */
        public Appointment findById(String id) {
            for (Appointment a : appointments) {
                if (a.getId().equalsIgnoreCase(id)) {
                    return a;
                }
            }
            return null;
        }

        /**
         * Removes an appointment by ID (cancels it) and saves the file.
         *
         * @param id the ID of the appointment to remove
         * @return true if it was removed, false if no appointment has that ID
         */
        public boolean remove(String id) {
            Appointment appointment = findById(id);
            if (appointment == null) {
                return false;
            }
            appointments.remove(appointment);
            save();
            return true;
        }

        /**
         * Saves the file. Call this after changing an appointment, such as a new date or time.
         */
        public void update() {
            save();
        }

        /**
         * Makes a sorted copy of the appointments: first by date, and for the same date, by time.
         * The original list is not changed.
         *
         * @return a new list of appointments in date and time order
         */
        public List<Appointment> sortedByDate() {
            List<Appointment> copy = new ArrayList<>(appointments);
            // comparing(getDate) sorts by date first. thenComparing(getTime) breaks ties using the time.
            copy.sort(Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getTime));
            return copy;
        }
    }

    /**
     * AppointmentMenu is the screen the user sees for the Appointment Booking module.
     * It shows the options, reads the choice, and calls the right action.
     *
     * @author Patcho
     */
    public static class AppointmentMenu {
        // The booking manager, and the patient manager (used to check that a patient exists).
        private AppointmentService service;
        private PatientService patientService;

        /**
         * Creates the menu.
         *
         * @param service the appointment service that keeps the bookings
         * @param patientService the patient service, used to check patient IDs
         */
        public AppointmentMenu(AppointmentService service, PatientService patientService) {
            this.service = service;
            this.patientService = patientService;
        }

        /**
         * Shows the Appointment Booking menu over and over, until the user picks 0.
         */
        public void show() {
            boolean running = true;
            while (running) {
                int choice = InputHelper.showMenu("APPOINTMENT BOOKING", "Back to main menu",
                        "Book a new appointment",
                        "Reschedule an appointment",
                        "Cancel an appointment",
                        "Appointment report (sorted by date)");

                // Run the action that matches the number the user typed.
                switch (choice) {
                    case 1: book(); break;
                    case 2: reschedule(); break;
                    case 3: cancel(); break;
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
        // OPTION 1: Book a new appointment
        // ============================================================
        /**
         * Asks for the patient, doctor, date and time, checks for a clash, then saves the booking.
         */
        private void book() {
            String patientId = InputHelper.readNonEmptyText("Patient ID: ");
            // Look up the patient. If there is no such patient, stop here.
            Patient patient = patientService.findById(patientId);
            if (patient == null) {
                System.out.println("No patient found with that ID. Register the patient first.");
                return;
            }
            String doctorName = InputHelper.readNonEmptyText("Doctor name: ");
            String date = InputHelper.readNonEmptyText("Appointment date (e.g. 2026-10-05): ");
            String time = InputHelper.readNonEmptyText("Appointment time (e.g. 14:30): ");
            // Refuse the booking if the doctor is already busy at that date and time.
            if (service.isDoubleBooked(doctorName, date, time)) {
                System.out.println("Could not book: Dr. " + doctorName + " is already booked at that date and time.");
                return;
            }
            try {
                // Build the booking, then save it.
                String id = service.generateId();
                service.add(new Appointment(id, patient, new Doctor(doctorName), date, time));
                System.out.println("Appointment booked with ID " + id);
            } catch (DoubleBookingException e) {
                System.out.println("Could not book appointment: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 2: Reschedule an appointment
        // ============================================================
        /**
         * Moves an existing appointment to a new date and time. It checks for a clash with other bookings,
         * but ignores the booking being moved, so keeping the same slot is allowed.
         */
        private void reschedule() {
            String id = InputHelper.readNonEmptyText("Enter appointment ID: ");
            Appointment appointment = service.findById(id);
            if (appointment == null) {
                System.out.println("No appointment found with that ID.");
                return;
            }
            String date = InputHelper.readNonEmptyText("New date: ");
            String time = InputHelper.readNonEmptyText("New time: ");
            // The fourth value is the appointment's own ID, so it is not counted as a clash with itself.
            if (service.isDoubleBooked(appointment.getDoctor().getName(), date, time, appointment.getId())) {
                System.out.println("Could not reschedule: that doctor is already booked at that date and time.");
                return;
            }
            try {
                // Change the date and time (checked by the setters), then save.
                appointment.setDate(date);
                appointment.setTime(time);
                service.update();
                System.out.println("Appointment rescheduled.");
            } catch (DoubleBookingException e) {
                System.out.println("Could not reschedule: " + e.getMessage());
            }
        }

        // ============================================================
        // OPTION 3: Cancel an appointment
        // ============================================================
        /**
         * Cancels (removes) an appointment by ID.
         */
        private void cancel() {
            String id = InputHelper.readNonEmptyText("Enter appointment ID to cancel: ");
            // Shows one message if the cancel worked, and another if no appointment has that ID.
            System.out.println(service.remove(id) ? "Appointment cancelled." : "No appointment found with that ID.");
        }

        // ============================================================
        // OPTION 4: Appointment report (sorted by date)
        // ============================================================
        /**
         * Prints every appointment in date and time order.
         */
        private void report() {
            List<Appointment> sorted = service.sortedByDate();
            if (sorted.isEmpty()) {
                System.out.println("No appointments to report.");
                return;
            }
            System.out.println("--- Appointment Report (sorted by date) ---");
            // Each appointment is a Record, so describe() works on it.
            for (Record r : sorted) {
                System.out.println(r.describe());
            }
        }
    }
}
