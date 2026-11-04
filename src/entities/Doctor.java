
package entities;

import resources.TreatmentRoom;
import resources.WaitingRoom;

import java.util.Random;

public class Doctor extends Thread {

    private final int doctorId;
    private final WaitingRoom waitingRoom;
    private final TreatmentRoom treatmentRoom;

    private final Random random = new Random();

    public Doctor(int doctorId, WaitingRoom waitingRoom,
                  TreatmentRoom treatmentRoom) {

        this.doctorId = doctorId;
        this.waitingRoom = waitingRoom;
        this.treatmentRoom = treatmentRoom;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            Patient patient = waitingRoom.getPatient();

            if (patient == null) {
                break;
            }

            System.out.println(
                    "Doctor " + doctorId +
                            " is assigned to " + patient.getPatientId()
            );

            System.out.println(
                    "Doctor " + doctorId +
                            " is trying to acquire a treatment room."
            );

            boolean roomAcquired = treatmentRoom.acquireRoom();

            if (roomAcquired) {
                boolean treatmentCompleted = false;
                try {
                    System.out.println(
                            "Doctor " + doctorId +
                                    " acquired a treatment room."
                    );

                    System.out.println(
                            "Doctor " + doctorId +
                                    " begins treating " + patient.getPatientId()
                    );

                    Thread.sleep(1000 + random.nextInt(2000));

                    System.out.println(
                            "Doctor " + doctorId +
                                    " finished treating " + patient.getPatientId()
                    );
                    treatmentCompleted = true;

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                } finally {

                    treatmentRoom.releaseRoom();

                    System.out.println(
                            "Doctor " + doctorId +
                                    " released the treatment room."
                    );

                }

                if (treatmentCompleted) {
                    patient.setTreated();

                    System.out.println(
                            "Doctor " + doctorId + " is now available."
                    );
                } else {
                    waitingRoom.addPatient(patient);
                }

            } else {

                System.out.println(
                        "Doctor " + doctorId +
                                " could not acquire a treatment room."
                );

                System.out.println(
                        "Doctor " + doctorId +
                                " is sending " + patient.getPatientId() +
                                " back to the waiting room."
                );

                waitingRoom.addPatient(patient);

                System.out.println(
                        "Doctor " + doctorId + " is now available."
                );
            }
        }
    }
}
