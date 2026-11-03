package resources;

import synchronization.Semaphore;

public class TreatmentRoom {

    private final Semaphore rooms;

    public TreatmentRoom() {
        rooms = new Semaphore(2); // cuz there are just two seats in the treatment room
    }

    public boolean acquireRoom() {
        return rooms.TimeAcquire(3000); // wait 3 sec 
    }

    public void releaseRoom() {
        rooms.release();
    }
}
