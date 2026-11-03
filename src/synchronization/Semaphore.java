package synchronization;

public class Semaphore {
    private int permit;

    public Semaphore(){
        permit = 0;
    }
    public Semaphore(int initial){
        permit = initial;
    }
    public synchronized void acquire(){
        permit--;
        while(permit < 0){
            try {
                wait();
            } catch (InterruptedException e) {
                permit++;
                throw new RuntimeException(e);
            }
        }
    }
    public synchronized boolean TimeAcquire(long time){
        permit--;
        long startTime = System.currentTimeMillis();
        while(permit < 0){


            long remaining = time - (System.currentTimeMillis() - startTime);
            if(remaining <= 0){
                permit++;
                return false;
            }
            try {
                wait(remaining);
            } catch (InterruptedException e) {
                permit++;
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public synchronized void release(){
        permit++;
        if (permit <= 0) notify();
    }

}
