# Distributed-System-MAP-Protocol-Mutual-Exclusion

## Project - 1

# Advanced Operating Systems Project

This repository contains my course project for **Advanced Operating Systems**, which implements distributed systems protocols across multiple nodes. The project is divided into four major parts, each building on the previous one.

---

## 📌 Project Overview

The project explores core concepts of distributed systems, including message passing, consistent global snapshots, logical time, and system termination detection. Nodes are simulated using sockets to represent bidirectional FIFO channels, and various protocols are implemented to manage distributed coordination.

---

## 🚀 Features

### Part 1: MAP Protocol (Message Passing)
- Implements a distributed system of `n` nodes (`0 ... n-1`) arranged in a configurable topology.
- Each node can be **active** or **passive**:
  - **Active nodes** send between `minPerActive` and `maxPerActive` messages before becoming passive.
  - Messages are sent to randomly selected neighbors with a minimum delay (`minSendDelay`).
  - **Passive nodes** become active upon receiving a message (unless they’ve reached their `maxNumber` limit).
- Communication:
  - All channels are **bidirectional**, **reliable**, and **FIFO**.
  - Channels are built using **TCP/SCTP sockets** that remain open until the program terminates.

---

### Part 2: Chandy–Lamport Global Snapshot Protocol
- Implements the **Chandy–Lamport algorithm** to record a consistent global snapshot of the system state.
- Node `0` initiates the snapshot process.
- Termination detection:
  - The MAP protocol is terminated when **all nodes are passive** and **all channels are empty**.
  - Snapshot results are converge-cast to node `0` over a spanning tree.

---

### Part 3: Vector Clock Verification
- Implements **Fidge/Mattern vector clocks** for logical time tracking.
- Vector timestamps are carried only by **application messages**.
- During snapshot collection, vector timestamps are used at node `0` to verify **consistency** of the snapshot.

---

### Part 4: System Termination
- Designs and implements a protocol to **bring all nodes to a halt** once node `0` detects MAP protocol termination.
- Ensures clean shutdown of all sockets and processes.

---

## ⚙️ How It Works

1. **Configuration File**  
   - Provides system parameters such as:
     - Number of nodes
     - Topology (neighbors)
     - MAP protocol parameters (`minPerActive`, `maxPerActive`, `minSendDelay`, `maxNumber`).
   - Used by each node at startup to initialize connections.

2. **Execution Flow**  
   - Nodes establish bidirectional FIFO channels.
   - MAP protocol runs with message exchanges until global termination is detected.
   - Snapshot protocol is used to monitor state and verify termination.
   - Vector clocks validate consistency.
   - Shutdown protocol halts all nodes gracefully.

---

## 🛠️ Technologies Used
- **Programming Language:** (Specify here, e.g., Java, C++, Python)
- **Networking:** TCP/SCTP sockets
- **Synchronization & Coordination:**  
  - MAP protocol  
  - Chandy–Lamport snapshot protocol  
  - Fidge/Mattern vector clocks  

---

## 📂 Project Structure
