package entities;

import resources.TreatmentRoom;
import resources.WaitingRoom;
import java.util.Random;

/**
 * Represents a doctor in the simulation. Each doctor runs as its own thread.
 * The doctor repeatedly:
 *   1. Retrieves a patient from the waiting room.
 *   2. Tries to acquire a treatment room for up to 3 seconds.
 *   3. If successful, treats the patient and releases the room.
 *   4. If the timeout expires, sends the patient back to the waiting room.
 */
public class Doctor extends Thread {
    private final int doctorId;
    private final WaitingRoom waitingRoom;
    private final TreatmentRoom treatmentRoom;
    private final Random random = new Random();

    public Doctor(int doctorId, WaitingRoom waitingRoom, TreatmentRoom treatmentRoom) {
        this.doctorId = doctorId;
        this.waitingRoom = waitingRoom;
        this.treatmentRoom = treatmentRoom;
    }

    @Override
    public void run() {
        while (true) {
            // 1. Get next patient (null indicates no more patients and doctors should stop)
            var patient = waitingRoom.getPatient();
            if (patient == null) {
                break; // exit the doctor thread gracefully
            }

            System.out.println("Doctor " + doctorId + " is assigned to " + patient.getPatientId() + ".");

            // 2. Try to acquire a treatment room (3‑second timeout)
            System.out.println("Doctor " + doctorId + " is trying to acquire a treatment room.");
            boolean roomAcquired = treatmentRoom.acquireRoom();

            if (roomAcquired) {
                // ----- Successful acquisition -----
                System.out.println("Doctor " + doctorId + " acquired a treatment room.");
                System.out.println("Doctor " + doctorId + " begins treating " + patient.getPatientId() + ".");

                // Simulate treatment time (1‑3 seconds random)
                try {
                    Thread.sleep(1000 + random.nextInt(2000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("Doctor " + doctorId + " finished treating " + patient.getPatientId() + ".");
                treatmentRoom.releaseRoom();
                System.out.println("Doctor " + doctorId + " released the treatment room.");

                // Notify patient that treatment is finished
                patient.setTreated();
                System.out.println("Doctor " + doctorId + " is now available.");
            } else {
                // ----- Timeout: cannot acquire a room -----
                System.out.println("Doctor " + doctorId + " could not acquire a treatment room.");
                System.out.println("Doctor " + doctorId + " is sending " + patient.getPatientId() + " back to the waiting room.");
                // Return patient to waiting room (addPatient prints full‑room messages if needed)
                waitingRoom.addPatient(patient);
                System.out.println("Doctor " + doctorId + " is now available.");
            }
        }
    }
}
