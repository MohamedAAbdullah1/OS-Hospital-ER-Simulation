
package entities;

import resources.WaitingRoom;
import java.util.function.Consumer;

public class Patient extends Thread {

    private final String id;
    private final WaitingRoom waitingRoom;
    private final Consumer<String> events;

    private volatile boolean treated = false;

    public Patient(String id, WaitingRoom waitingRoom) {
        this(id, waitingRoom, System.out::println);
    }

    public Patient(String id, WaitingRoom waitingRoom, Consumer<String> events) {
        this.id = id;
        this.waitingRoom = waitingRoom;
        this.events = events;
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

        events.accept(id + " arrived and joined the waiting room.");

        waitingRoom.addPatient(this);
        events.accept(id + " is waiting for a doctor.");

        waitForTreatment();

        if (treated) {
            events.accept(id + " was treated and left.");
        }
    }
}
