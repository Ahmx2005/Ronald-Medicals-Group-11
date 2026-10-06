package ug.ac.vu.g11.app;

// "import" tells Java where to find the classes we use in this file.
// Each line names a class and the package (folder) it lives in, for example
// ug.ac.vu.g11.patient.PatientModule.PatientService is the PatientService class inside the patient module.
import ug.ac.vu.g11.appointment.AppointmentModule.AppointmentMenu;
import ug.ac.vu.g11.appointment.AppointmentModule.AppointmentService;
import ug.ac.vu.g11.billing.BillingModule.BillingMenu;
import ug.ac.vu.g11.billing.BillingModule.BillingService;
import ug.ac.vu.g11.core.Core.InputHelper;
import ug.ac.vu.g11.labtest.LabTestModule.LabTestMenu;
import ug.ac.vu.g11.labtest.LabTestModule.LabTestService;
import ug.ac.vu.g11.patient.PatientModule.PatientMenu;
import ug.ac.vu.g11.patient.PatientModule.PatientService;
import ug.ac.vu.g11.prescription.PrescriptionModule.PrescriptionMenu;
import ug.ac.vu.g11.prescription.PrescriptionModule.PrescriptionService;
import ug.ac.vu.g11.stock.StockModule.MedicationService;
import ug.ac.vu.g11.stock.StockModule.StockMenu;
import ug.ac.vu.g11.triage.TriageModule.ConsultationService;
import ug.ac.vu.g11.triage.TriageModule.TriageMenu;

/**
 * MAIN: this is where the Ronald Medicals program starts running. Owner: David and Ethar (integration leads).
 *
 * What this class does, in order:
 *   1. Creates one "service" for each module. A service is the part of a module that keeps
 *      its list of records and loads them from a file. Creating a service loads its saved file.
 *   2. Creates one "menu" for each module. A menu is the screen the user sees for that module.
 *   3. Shows the main menu in a loop, and sends the user to the chosen module's menu.
 *
 * Why the order matters: some modules need another module's data. For example, an appointment
 * must belong to a real patient, so the patient service must be created before the appointment service.
 *
 * In Java, a "class" is a blueprint for objects. The "main" method below is the special starting
 * point: when you run the program, Java looks for this method and runs the code inside it, top to bottom.
 *
 * @author David and Ethar
 */
public class Main {

    /**
     * The starting point of the whole program. Java runs this method first.
     *
     * @param args extra text typed after the program name on the command line. We do not use it, so it is ignored.
     */
    public static void main(String[] args) {
        // Shows the title once, when the program starts.
        System.out.println("=== Ronald Medicals - Clinic Management System (Group 11) ===");

        // Step 1: create the services. Each "new" call builds a service and loads its saved file.
        // The order is important, because some services need the ones created before them.
        PatientService patientService = new PatientService();
        AppointmentService appointmentService = new AppointmentService(patientService);
        ConsultationService consultationService = new ConsultationService(patientService);
        MedicationService medicationService = new MedicationService();
        PrescriptionService prescriptionService = new PrescriptionService(patientService, medicationService);
        LabTestService labTestService = new LabTestService(patientService);
        BillingService billingService = new BillingService(
                patientService, consultationService, prescriptionService, labTestService);

        // Step 2: create the menus. Each menu is given the services it needs to do its work.
        // The line below is more complex: it passes in a small "rule" (written as a lambda, which means
        // "a short unnamed function"). The rule takes a patient ID and answers the question
        // "does any appointment, consultation, prescription, lab test or invoice belong to this patient?"
        // The patient menu uses that answer to refuse removing a patient who still has records.
        // stream() looks through each list one item at a time, and anyMatch() says yes if at least one item matches.
        PatientMenu patientMenu = new PatientMenu(patientService, id ->
                appointmentService.getAll().stream().anyMatch(a -> a.getPatient().getId().equalsIgnoreCase(id))
                || consultationService.getAll().stream().anyMatch(c -> c.getPatient().getId().equalsIgnoreCase(id))
                || prescriptionService.getAll().stream().anyMatch(p -> p.getPatient().getId().equalsIgnoreCase(id))
                || labTestService.getAll().stream().anyMatch(t -> t.getPatient().getId().equalsIgnoreCase(id))
                || billingService.getAll().stream().anyMatch(i -> i.getPatient().getId().equalsIgnoreCase(id)));
        AppointmentMenu appointmentMenu = new AppointmentMenu(appointmentService, patientService);
        TriageMenu triageMenu = new TriageMenu(consultationService, patientService);
        PrescriptionMenu prescriptionMenu = new PrescriptionMenu(prescriptionService, patientService, medicationService);
        LabTestMenu labTestMenu = new LabTestMenu(labTestService, patientService);
        StockMenu stockMenu = new StockMenu(medicationService);
        BillingMenu billingMenu = new BillingMenu(
                billingService, patientService, consultationService, prescriptionService, labTestService);

        // Step 3: the main menu loop. "running" is true while the program should keep going.
        boolean running = true;
        // "while" repeats the code inside the braces as long as the condition in brackets is true.
        while (running) {
            // Draws the main menu and waits for the user to type a number. The result is stored in "choice".
            int choice = InputHelper.showMenu("RONALD MEDICALS - MAIN MENU", "Exit",
                    "Patients",
                    "Appointment Booking",
                    "Consultation",
                    "Prescription",
                    "Lab Tests",
                    "Medication Stock",
                    "Billing");

            // "switch" checks the chosen number and runs the matching block.
            // "break" leaves the switch once that block is done, so the next case does not run too.
            switch (choice) {
                case 1: patientMenu.show(); break;
                case 2: appointmentMenu.show(); break;
                case 3: triageMenu.show(); break;
                case 4: prescriptionMenu.show(); break;
                case 5: labTestMenu.show(); break;
                case 6: stockMenu.show(); break;
                case 7: billingMenu.show(); break;
                case 0:
                    // Choosing 0 sets running to false, so the while loop stops and the program ends.
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                // "default" runs when the number matches none of the cases above, such as 9.
                default:
                    System.out.println("Please choose a number from the menu.");
            }
        }
    }
}
