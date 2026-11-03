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

        while(permit == 0){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        permit--;
    }

    public synchronized boolean TimeAcquire(long time){

        long startTime = System.currentTimeMillis();

        while(permit == 0){

            long remaining = time -
                    (System.currentTimeMillis() - startTime);

            if(remaining <= 0){
                return false;
            }

            try {
                wait(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        permit--;
        return true;
    }

    public synchronized void release(){

        permit++;
        notifyAll();
    }
}