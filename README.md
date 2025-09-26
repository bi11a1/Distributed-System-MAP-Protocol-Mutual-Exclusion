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

#### Part 4: System Termination
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

For details about individual scripts, see **`ScriptsReadme.txt`** inside Project - 1.

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
