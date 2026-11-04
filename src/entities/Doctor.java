
package entities;

import resources.TreatmentRoom;
import resources.WaitingRoom;

import java.util.Random;
import java.util.function.Consumer;

public class Doctor extends Thread {

    private final int doctorId;
    private final WaitingRoom waitingRoom;
    private final TreatmentRoom treatmentRoom;
    private final Consumer<String> events;

    private final Random random = new Random();

    public Doctor(int doctorId, WaitingRoom waitingRoom,
                  TreatmentRoom treatmentRoom) {
        this(doctorId, waitingRoom, treatmentRoom, System.out::println);
    }

    public Doctor(int doctorId, WaitingRoom waitingRoom,
                  TreatmentRoom treatmentRoom, Consumer<String> events) {
        this.doctorId = doctorId;
        this.waitingRoom = waitingRoom;
        this.treatmentRoom = treatmentRoom;
        this.events = events;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            Patient patient = waitingRoom.getPatient();

            if (patient == null) {
                break;
            }

            events.accept(
                    "Doctor " + doctorId +
                            " is assigned to " + patient.getPatientId()
            );

            events.accept(
                    "Doctor " + doctorId +
                            " is trying to acquire a treatment room."
            );

            boolean roomAcquired = treatmentRoom.acquireRoom();

            if (roomAcquired) {
                boolean treatmentCompleted = false;
                try {
                    events.accept(
                            "Doctor " + doctorId +
                                    " acquired a treatment room."
                    );

                    events.accept(
                            "Doctor " + doctorId +
                                    " begins treating " + patient.getPatientId()
                    );

                    Thread.sleep(1000 + random.nextInt(2000));

                    events.accept(
                            "Doctor " + doctorId +
                                    " finished treating " + patient.getPatientId()
                    );
                    treatmentCompleted = true;

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                } finally {

                    treatmentRoom.releaseRoom();

                    events.accept(
                            "Doctor " + doctorId +
                                    " released the treatment room."
                    );

                }

                if (treatmentCompleted) {
                    patient.setTreated();

                    events.accept(
                            "Doctor " + doctorId + " is now available."
                    );
                } else {
                    waitingRoom.addPatient(patient);
                }

            } else {

                events.accept(
                        "Doctor " + doctorId +
                                " could not acquire a treatment room."
                );

                events.accept(
                        "Doctor " + doctorId +
                                " is sending " + patient.getPatientId() +
                                " back to the waiting room."
                );

                waitingRoom.addPatient(patient);

                events.accept(
                        "Doctor " + doctorId + " is now available."
                );
            }
        }
    }
}
