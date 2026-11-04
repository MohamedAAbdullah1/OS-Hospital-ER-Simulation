
package entities;

import resources.WaitingRoom;

public class Patient extends Thread {

    private final String id;
    private final WaitingRoom waitingRoom;

    private volatile boolean treated = false;

    public Patient(String id, WaitingRoom waitingRoom) {
        this.id = id;
        this.waitingRoom = waitingRoom;
    }

    public String getPatientId() {
        return id;
    }
    public boolean isTreated(){
        return treated;
    }

    public synchronized void setTreated() {
        treated = true;
        notifyAll();
    }

    private synchronized void waitForTreatment() {

        while (!treated) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void run() {

        System.out.println(id + " arrived.");

        waitingRoom.addPatient(this);

        waitForTreatment();

        if (treated) {
            System.out.println(id + " left.");
        }
    }
}
