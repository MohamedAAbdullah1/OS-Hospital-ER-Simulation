import entities.Doctor;
import entities.Patient;
import resources.TreatmentRoom;
import resources.WaitingRoom;

import java.util.Scanner;
import java.util.function.Consumer;

public class Hospital {

    public static void main(String[] args) throws InterruptedException {
        int waitingRoomCapacity, numberOfDoctors, numberOfPatients;
        Scanner sc = new Scanner(System.in);

        System.out.print("Waiting room capacity: ");
        waitingRoomCapacity = sc.nextInt();
        System.out.print("Number of doctors: ");
        numberOfDoctors = sc.nextInt();
        System.out.print("Number of patients: ");
        numberOfPatients = sc.nextInt();


        runSimulation(waitingRoomCapacity, numberOfDoctors, numberOfPatients, System.out::println);
    }

    public static void runSimulation(int waitingRoomCapacity, int numberOfDoctors,
                                     int numberOfPatients, Consumer<String> events)
            throws InterruptedException {
        if (waitingRoomCapacity < 1 || numberOfDoctors < 1 || numberOfPatients < 1) {
            throw new IllegalArgumentException("All values must be at least 1.");
        }
        WaitingRoom waitingRoom = new WaitingRoom(waitingRoomCapacity);
        TreatmentRoom treatmentRoom = new TreatmentRoom();

        Doctor[] doctors = new Doctor[numberOfDoctors];
        Patient[] patients = new Patient[numberOfPatients];

        for (int i = 0;i < numberOfDoctors;i++) {
            doctors[i] = new Doctor(i+1, waitingRoom,treatmentRoom, events);
        }
        for (int i = 0;i < numberOfPatients;i++) {
            patients[i] = new Patient("P" + (i+1), waitingRoom, events);
        }

        for (int i = 0;i < numberOfDoctors;i++) {
            doctors[i].start();
        }

        for (int i = 0;i < numberOfPatients;i++) {
            patients[i].start();
        }

        for (int i = 0;i < numberOfPatients;i++) {
            patients[i].join();
        }

        for (int i = 0;i < numberOfDoctors;i++) {
            waitingRoom.addPatient(null);
        }

        for (int i = 0;i < numberOfDoctors;i++) {
            doctors[i].join();
        }
        events.accept("Simulation complete. All patients have been treated.");
    }
}
