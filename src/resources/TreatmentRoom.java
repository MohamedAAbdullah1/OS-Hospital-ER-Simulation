package resources;

import synchronization.Semaphore;

public class TreatmentRoom {

    private final Semaphore rooms;

    public TreatmentRoom() {
        rooms = new Semaphore(2); 
    }

    public boolean acquireRoom() {
        return rooms.TimeAcquire(3000); 
    }

    public void releaseRoom() {
        rooms.release();
    }
}
