package resources;

import entities.Patient;
import synchronization.Semaphore;

public class WaitingRoom {

    private final int capacity;
    private  int inputPtr = 0;
    private  int outputPtr = 0;

    private final Semaphore spaces;
    private final Semaphore elements;
    private final Semaphore mutex;

    private final Patient[] patients;

    public WaitingRoom(int capacity) {
        this.capacity = capacity;
        spaces = new Semaphore(capacity);
        elements = new Semaphore(0);
        mutex = new Semaphore(1);
        patients = new Patient[capacity];
    }

    public void addPatient(Patient patient){
        spaces.acquire();
        mutex.acquire();
        patients[inputPtr] = patient;
        inputPtr = (inputPtr + 1) % capacity;
        mutex.release();
        elements.release();
    }
    public Patient getPatient(){
        elements.acquire();
        mutex.acquire();
        Patient patient = patients[outputPtr];
        patients[outputPtr] = null;
        outputPtr = (outputPtr + 1 ) % capacity;
        mutex.release();
        spaces.release();
        return patient;
    }
}