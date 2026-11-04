# OS Hospital ER Simulation

A Java simulation of an emergency department. Patients enter a bounded waiting
room, doctors treat them using two shared treatment rooms, and events are
reported as the threads run.

## Launch the desktop interface

Compile and launch from the project root:

```sh
javac -d out src/Hospital.java src/HospitalGUI.java src/entities/*.java src/resources/*.java src/synchronization/*.java
java -cp out HospitalGUI
```

Choose the waiting room capacity, number of doctors, and number of patients,
then select **Start simulation**. The activity feed shows arrivals, assignments,
treatment progress, and departures.

## Launch the console version

Compile with the command above, then run `java -cp out Hospital` and enter the
same three simulation values when prompted.
