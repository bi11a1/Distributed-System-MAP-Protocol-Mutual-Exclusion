# Advanced Operating Systems Projects

## Project 1 (Message Passing, Consistent Global Snapshots, Termination Detection)

### Project Overview

The project explores core concepts of distributed systems, including message passing, consistent global snapshots, logical time, and system termination detection. Nodes are simulated using sockets to represent bidirectional FIFO channels, and various protocols are implemented to manage distributed coordination. The project is divided into four major parts, each building on the previous one.

---

#### Part 1: MAP Protocol (Message Passing)
- Implements a distributed system of `n` nodes (`0 ... n-1`) arranged in a configurable topology.
- Each node can be **active** or **passive**:
  - **Active nodes** send between `minPerActive` and `maxPerActive` messages before becoming passive.
  - Messages are sent to randomly selected neighbors with a minimum delay (`minSendDelay`).
  - **Passive nodes** become active upon receiving a message (unless they’ve reached their `maxNumber` limit).
- Communication:
  - All channels are **bidirectional**, **reliable**, and **FIFO**.
  - Channels are built using **TCP/SCTP sockets** that remain open until the program terminates.
- Implementation detail: Node `0` is always active at the start; the rest of the nodes start active with 50% probability.

---

#### Part 2: Chandy–Lamport Global Snapshot Protocol
- Implements the **Chandy–Lamport algorithm** to record a consistent global snapshot of the system state.
- Node `0` initiates the snapshot process.
- Termination detection:
  - The MAP protocol is terminated when **all nodes are passive** and **all channels are empty**.
  - Snapshot results are converge-cast to node `0` over a spanning tree.
- Verification of global consistency is done by checking the recorded local states.

---

#### Part 3: Fidge/Mattern Vector Clocks
- Implements **Fidge/Mattern vector clocks** for logical time tracking.
- Vector timestamps are carried only by **application messages**.
- Each node records its **local vector clock** during a snapshot.
- At node `0`, vector timestamps are used to verify snapshot **consistency**.

---

#### Part 4: Termination Detection
- When all nodes are passive and no in-transit messages remain, the system is considered terminated.
- A custom shutdown protocol halts all nodes and closes all ports and streams gracefully.

---

### Implementation Notes
- **Language:** Java  
- **Entry point:** `Main.java`  
- Two parallel threads are used:
  - One for running the MAP protocol.
  - One for managing snapshots.  
- Code is well-commented to improve readability.  
- Default configuration file: `config.txt`  
  - To use a different file, update the file name and path in the scripts.

For details about individual scripts, see **`ScriptsReadme.txt`** inside Project 1.

---

### Running the Project

#### Build & Launch
```
./build.sh
./launcher.sh
```

#### Cleanup After Execution Ends
```
./cleanup.sh
./cleanFiles.sh
```

## Project 2 (Mutual Exclusion in Distributed Systems)

---

### Project Overview

The project implements a mutual exclusion service based on **Roucairol and Carvalho’s algorithm**. The service provides mutual exclusion among `n` distributed processes, ensuring that **at most one process** is in its critical section at any given time. It uses two key API calls:

- **`cs-enter()`**  
  Blocks until the invoking process is granted permission to enter its critical section.  

- **`cs-leave()`**  
  Notifies the service that the invoking process has finished executing its critical section.  

The implementation is structured into two interacting modules per process:

1. **Application Module**  
   - Generates requests to enter critical sections.  
   - Executes critical sections after permission is granted.  
   - Modeled using two parameters:  
     - `d`: *inter-request delay* (time between leaving one CS and requesting the next).  
     - `c`: *CS execution time*.  
   - Both `d` and `c` are drawn from **exponential distributions**.

2. **Mutual Exclusion Service Module**  
   - Implements Roucairol and Carvalho’s algorithm.  
   - Coordinates distributed processes to ensure mutual exclusion using request, reply, and release messages.  
   - Handles all message-passing logic over reliable FIFO channels.

---

### Implementation Notes

- **Language:** Java  
- **Entry point:** `Main.java`  
- Default configuration file: `config.txt`  
  - If a different config file is used, update the filename/path in the scripts.  
- The program is well-commented to improve readability.
- 
---

### Testing Mechanism

A **testing mechanism** using vector clock verifies correctness:  
- Ensures that **at most one process** is in its critical section at any time.  
- Detects violations automatically (no manual log inspection required).  

---

### Experimental Evaluation

The project includes experiments to evaluate performance across different system parameters:

- **Parameters Varied:**  
  - Number of processes (`n`)  
  - Inter-request delay (`d`)  
  - CS execution time (`c`)  

- **Metrics Measured:**  
  - **Message complexity** (number of messages per CS execution)  
  - **Response time** (delay between request and CS entry)  
  - **System throughput** (rate of CS completions)  

- **Results:**  
  - Multiple runs are averaged for each configuration.  
  - Results are plotted as graphs.  
  - **Raw data of experiments** is provided in **`Experimental Data.xlsx`**.  

---

### Running the Project

#### Build & Launch
```
./build.sh
./launcher.sh
```
#### Cleanup After Execution Ends
```
./cleanup.sh
./cleanFiles.sh
```
