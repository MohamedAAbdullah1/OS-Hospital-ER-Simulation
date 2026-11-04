import entities.Doctor;
import entities.Patient;
import resources.TreatmentRoom;
import resources.WaitingRoom;

import java.util.Scanner;

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


        WaitingRoom waitingRoom = new WaitingRoom(waitingRoomCapacity);
        TreatmentRoom treatmentRoom = new TreatmentRoom();

        Doctor[] doctors = new Doctor[numberOfDoctors];
        Patient[] patients = new Patient[numberOfPatients];

        for (int i = 0;i < numberOfDoctors;i++) {
            doctors[i] = new Doctor(i+1, waitingRoom,treatmentRoom);
        }
        for (int i = 0;i < numberOfPatients;i++) {
            patients[i] = new Patient("P" + (i+1), waitingRoom);
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
    }
}
