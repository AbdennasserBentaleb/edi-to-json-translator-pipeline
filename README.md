# EDI to JSON Translator Pipeline

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Camel](https://img.shields.io/badge/Apache%20Camel-4.8.2-orange.svg)](https://camel.apache.org/)

A 12-Factor microservice built to ingest legacy Electronic Data Interchange (EDI) documents (CSV and XML) from local file systems, translate them into canonical JSON, and egress them to downstream REST APIs. 

## Tech Stack

- **Language:** Java 17
- **Frameworks:** Spring Boot 3.4.1, Apache Camel 4.8.2
- **Containerization & Orchestration:** Docker, Kubernetes (K8s)
- **API Documentation:** OpenAPI (Swagger UI)

## High-Level Architecture

The pipeline uses an `inbox/` directory as the ingress point. Apache Camel routes pick up `.csv` or `.xml` files, map them to a strongly-typed `CanonicalPayload`, marshal them to JSON, and POST them to an egress endpoint. Files are consumed idempotently and moved to a Dead Letter Channel (`error/`) upon unhandled translation exceptions.

See the complete [ARCHITECTURE.md](ARCHITECTURE.md) and [PRD.md](PRD.md) for more details.

## Architecture Decisions & Trade-offs

1. Local File System Polling vs. Event-Driven Architecture:
   - Decision: The pipeline uses Apache Camel's File component to poll `inbox/`.
   - Trade-off: Polling introduces latency compared to event-based ingress (e.g., AWS S3 Event Notifications or Kafka logs). This design favors simplicity for environments where legacy systems can only push files over SFTP or SMB mounts. However, it requires careful tuning of the polling interval and thread limits to prevent CPU jitter under heavy load.
2. In-Memory DOM vs. SAX Streaming for XML:
   - Decision: The application uses Jackson XML data binding, which loads the XML into memory for transformation.
   - Trade-off: While easier to maintain than a custom SAX parser, this introduces a memory limit. Attempting to process XML files larger than 100MB could trigger OutOfMemory (OOM) errors or significant GC pauses in Java 17. For massive batches, a split-iterator EIP pattern should be adopted.
3. Internal Synchronization Engine:
   - Decision: A `synchronizedList` serves as the test sink for the mock REST endpoint. 
   - Challenge Faced: Designing high-concurrent test suites around this revealed potential race conditions during heavy throughput testing, necessitating the implementation of explicit `java.util.concurrent.CountDownLatch` stress-testing logic, alongside `AtomicInteger` counters to guarantee thread safety during 99th percentile load simulations.
4. Local Deployment via Docker Compose:
   - Decision: Bundling the entire application stack in a single `docker-compose.yml` without introducing a heavyweight persistent datastore or external broker.
   - Trade-off: Simplifies the execution profile for immediate local testing. It requires mapping external storage volumes carefully (`subPath` mapping) to prevent masking critical application JAR directories in Kubernetes pod environments.

## Running Locally

To reduce reviewer friction, the system provides a true one-click local setup via Docker Compose, eliminating the need to install external brokers, pull language models, or provision databases.

### Quick Start (Docker Compose)

1. **Start the Pipeline**
   ```bash
   docker-compose up --build -d
   ```
2. **Access the Dashboard**
   Navigate to: `http://localhost:8080`
3. **View API Documentation**
   Swagger UI is available at `http://localhost:8080/swagger-ui.html`
4. **Test the Flow**
   Upload a `.csv` or `.xml` file through the dashboard UI or drop a test file directly into the local `./inbox` directory created by Docker Compose. Watch the mocked JSON egress in the real-time Vanilla JS dashboard.

### Alternative: Bare Metal / Maven

```bash
./mvnw clean package
java -jar target/edi-translator-pipeline-1.0.0.jar
```

## Testing & K8s Hardening

- Concurrency and routing logic are tested using `./mvnw clean test`. 
- Kubernetes Deployment manifests (`k8s/deployment.yaml`) are hardened to drop all capabilities, enforce read-only filesystems, and run under a non-root UID (`65532`). Spring Boot Actuator endpoints govern Liveness/Readiness probes.


