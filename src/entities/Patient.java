package entities;

import resources.WaitingRoom;

/**
 * Represents a patient in the simulation. Each patient runs as its own thread.
 * The patient:
 *   1. Announces arrival.
 *   2. Tries to enter the waiting room (the WaitingRoom class handles the
 *      "full" messages).
 *   3. Waits until a doctor marks the patient as treated.
 *   4. Announces departure.
 */
public class Patient extends Thread {
    private final String id;                     // e.g. "P1"
    private final WaitingRoom waitingRoom;
    private volatile boolean treated = false;    // set by Doctor when treatment finishes

    public Patient(String id, WaitingRoom waitingRoom) {
        this.id = id;
        this.waitingRoom = waitingRoom;
    }

    public String getPatientId() {
        return id;
    }

    /** Called by Doctor when the patient has been treated. */
    public synchronized void setTreated() {
        this.treated = true;
        notifyAll();
    }

    /** Wait until Doctor invokes {@link #setTreated()}. */
    private synchronized void waitForTreatment() {
        while (!treated) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        // 1. announce arrival
        System.out.println(id + " arrived.");

        // 2. try to enter the waiting room (the method prints full/enter messages)
        waitingRoom.addPatient(this);

        // 3. wait until a Doctor finishes treatment
        waitForTreatment();

        // 4. announce leaving the hospital
        System.out.println(id + " left.");
    }
}
