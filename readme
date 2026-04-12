# Real-Time Fraud Engine: Stateful Streaming with Kafka Streams & Interactive Queries

This project is a highly scalable, real-time fraud detection engine built with **Kafka Streams**. It demonstrates an advanced "Database-less" (Stateful Streaming) architecture. Instead of pushing data to an external database (like Postgres or Redis) for querying, the application maintains its own distributed state using **RocksDB** and exposes that state directly via **Interactive Queries (IQ)** and **RPC proxying**.

## 🧠 The Concept

The system tracks credit card swipes in real-time. If a user swipes their card **3 or more times within a 5-minute sliding window**, the system instantly flags the user as "BLOCKED" and emits a fraud alert.

### Why Skip the Database?
Traditional architectures write events to a database, and a background worker or API queries that database (adding network hops, disk I/O, and locking overhead). 

By skipping the database and processing state in-stream:
* **Zero Read-During-Write Penalty:** State is updated in memory ($O(1)$) as events arrive, avoiding expensive SQL `COUNT` queries over millions of rows.
* **Millisecond Freshness:** Fraud checks happen instantly. Traditional DB polling creates a "staleness gap" where fraudsters can slip through.
* **High Throughput:** Decouples ingestion (returning `202 Accepted` instantly) from the complex event processing.

## 🔬 Distributed Systems Concepts & Edge Cases

Building a distributed database using Kafka Streams introduces specific operational challenges. This architecture explicitly handles the following scenarios:

### 1. Partitioning & Data Locality
Kafka uses a consistent hashing algorithm (`hash(user_id) % num_partitions`) to ensure all swipes for a specific user ALWAYS go to the exact same partition, and therefore the same application instance. This guarantees that local RocksDB aggregations are globally accurate for that user.

### 2. Node Creation & Scaling Up (The Rebalance)
When traffic spikes and the cluster is scaled (e.g., from 3 nodes to 5), the system undergoes a specific lifecycle:
* **The Rebalance Handshake:** The Kafka Group Coordinator pauses the cluster and recalculates partition ownership. Node 1 and Node 2 will relinquish some of their partitions to give to the new nodes.
* **Avoiding Data Loss (The Changelog):** Node 1 *does not* send its RocksDB files over the network to Node 5. Instead, Node 5 looks at its newly assigned partitions, connects to the hidden **Kafka Changelog Topic**, and "replays" the entire historical state until its own local RocksDB is a perfect mirror of what Node 1 previously held.
* **IQ Routing During Scale:** While Node 5 is "warming up," the Interactive Query API dynamically detects `KeyQueryMetadata.NOT_AVAILABLE` and returns a `503 Service Unavailable` rather than returning false/empty data, ensuring strict eventual consistency.

### 3. Instance Failure & Fault Tolerance
In a distributed system, nodes will eventually crash. If Node 2 dies suddenly:
* **Detection:** The Kafka Broker notices Node 2 has stopped sending heartbeats and triggers a rebalance.
* **Reassignment:** Node 2's partitions are instantly reassigned to Node 1 and Node 3.
* **The "Standby Replica" Advantage:** To prevent Node 1 from taking minutes to download Node 2's changelog data, this application is configured with `num.standby.replicas=1`. Node 1 was already keeping a silent, "shadow copy" of Node 2's RocksDB state on its disk. When Node 2 dies, Node 1 promotes its shadow copy to primary and resumes processing in milliseconds.

### 4. Complex Logic & "Zombies"
* **Complex State:** RocksDB is not limited to integers. For complex fraud rules (e.g., velocity + geography), we store complete `UserSession` POJOs in RocksDB to evaluate multi-variable logic in microseconds.
* **Zombies (Unblocking):** Standard windowed aggregations retain data for the window duration. For explicit unblocking actions, a separate manual-override topic or a state TTL (Time-To-Live) mechanism must be implemented to prune stale state from RocksDB.

---

## 🏗️ Architecture Overview



1. **Frontend Layer:** A lightweight web UI simulates card swipes and requests live user status.
2. **API Gateway (Nginx):** Acts as a load balancer, routing UI requests via Round-Robin.
3. **Event Ingestion:** The REST API acts as a Kafka Producer, publishing raw transactions to a partitioned Kafka topic.
4. **Stateful Stream Processing (3 Nodes):** * Each node processes a subset of the data based on `user_id` partitioning.
    * Calculates the 5-minute sliding window using `WindowStore` (RocksDB).
5. **Interactive Queries & RPC:** * If a user requests the status of `user_A` from Node 1, but Node 3 owns `user_A`'s data, Node 1 dynamically discovers this using Kafka's Metadata API.
    * Node 1 acts as an RPC proxy, forwarding the HTTP request to Node 3, and returning the result seamlessly.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **Framework:** Micronaut (for lightweight, fast-booting embedded REST/Kafka integration)
* **Messaging/Event Streaming:** Redpanda (Kafka-compatible, Zookeeper-less)
* **Stream Processing:** Kafka Streams API
* **Local State Store:** RocksDB
* **Infrastructure:** Docker & Docker Compose
* **Frontend:** Vanilla JavaScript/HTML

---

## 📂 Project Structure

```text
fraud-engine/
├── docker-compose.yml              # Defines Redpanda, Nginx, and App 1-3
├── nginx.conf                      # Round-robin load balancer config
├── frontend/
│   └── index.html                  # Simple UI dashboard
└── backend/
    ├── Dockerfile                  # Builds the backend application
    ├── build.gradle.kts            # Dependencies
    └── src/main/kotlin/com/example/
        ├── Application.kt          # App Entry Point
        ├── Transaction.kt          # Data Models (Transaction, FraudStatus)
        ├── FraudStream.kt          # Kafka Streams Topology (The Windowing Logic)
        └── FraudController.kt      # Interactive Queries & RPC Proxy logic