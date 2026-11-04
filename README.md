# Hospital Emergency Room Simulation

A Java implementation of a hospital emergency room simulation developed for the Operating Systems course.

The program models patients arriving at a hospital, waiting for an available doctor, and receiving treatment when one of the hospital's two treatment rooms becomes available. It uses multiple threads and custom synchronization mechanisms to coordinate access to shared resources.

## Overview

The simulation consists of three main components:

- **Patients:** Each patient runs in a separate thread, enters the waiting room, and waits until treatment is completed.
- **Doctors:** Each doctor runs independently, retrieves patients from the waiting room, and attempts to acquire a treatment room.
- **Hospital resources:** A bounded waiting room and two treatment rooms shared among all doctors.

If a doctor cannot acquire a treatment room within three seconds, the patient is returned to the waiting room.

## Implementation

### Waiting Room

The waiting room is implemented as a bounded circular buffer using a `Patient` array.

Three semaphores coordinate access to the buffer:

| Semaphore | Initial value | Purpose |
|---|---:|---|
| `spaces` | Capacity | Tracks available positions. |
| `elements` | 0 | Tracks waiting patients. |
| `mutex` | 1 | Ensures exclusive access to the buffer. |

Patients wait when the buffer is full, while doctors wait when it is empty.

### Treatment Rooms

The hospital has two treatment rooms, managed by a semaphore initialized with two permits.

A doctor must acquire a permit before treatment begins. The acquisition uses a three-second timeout. If it fails, the patient is placed back in the waiting room.

The permit is released after treatment, including when the doctor is interrupted during treatment.

### Custom Semaphore

The `Semaphore` class is implemented using Java's intrinsic synchronization mechanisms:

- `synchronized`
- `wait()`
- `notify()`

It provides three operations:

- `acquire()` — waits until a permit is available.
- `TimeAcquire(long time)` — attempts to acquire a permit within a specified time.
- `release()` — returns a permit and notifies a waiting thread.

### Thread Termination

After all patient threads have completed, the hospital inserts one `null` entry for each doctor into the waiting room.

Doctors interpret these entries as termination signals and exit their execution loops. The main thread then joins all doctor threads before the program terminates.

## Project Structure

```text
src/
├── Hospital.java
├── entities/
│   ├── Doctor.java
│   └── Patient.java
├── resources/
│   ├── WaitingRoom.java
│   └── TreatmentRoom.java
└── synchronization/
    └── Semaphore.java
```

## Requirements

- JDK 21
- IntelliJ IDEA or another Java IDE

No external libraries are required.

## Running the Application

1. Clone the repository:

   ```bash
   git clone <repository-url>
   ```

2. Open the project in your IDE.
3. Set the Project SDK and compiler target to Java 21.
4. Run `Hospital.java`.

The program requests three input values:

```text
Waiting room capacity: 2
Number of doctors: 3
Number of patients: 5
```

The execution output shows patient arrivals, doctor assignments, treatment room acquisition, treatment completion, and patient departures.

Since the application is multithreaded, the order of printed messages may differ between executions.

## Course

**Operating Systems — Assignment 1**

Faculty of Computers and Artificial Intelligence, Cairo University.

**Language:** Java 21
