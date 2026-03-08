# EDI to JSON Translator Pipeline

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Camel](https://img.shields.io/badge/Apache%20Camel-4.0.0-orange.svg)](https://camel.apache.org/)

A robust, 12-Factor App microservice designed to modernize legacy B2B integration patterns. Built with Spring Boot and Apache Camel, this pipeline polls a local file system for legacy Electronic Data Interchange (EDI) documents (CSV and XML), translates them into a canonical JSON format, and forwards them to a downstream REST API.

## Features

- **Enterprise Integration Patterns (EIP):** Leverages Apache Camel for Content-Based Routing and robust file polling.
- **Idempotency:** Ensures files are only processed once by moving them to success directories or `.camel` tracking folders.
- **Fault Tolerance:** Implements a Dead Letter Channel to quarantine invalid payloads into an `error/` directory without crashing the route.
- **Real-Time Dashboard:** A modern, Vanilla JS frontend to monitor ingested payloads and upload test documents dynamically.
- **Kubernetes Ready:** Includes K8s manifests for zero-downtime deployments.

## Architecture

The pipeline uses an `inbox/` directory as the ingress point. Camel routes pick up `.csv` or `.xml` files, maps them to a strongly-typed `CanonicalPayload`, marshals them to JSON, and POSTs them to the mock egress endpoint.

See the complete [ARCHITECTURE.md](ARCHITECTURE.md) and [PRD.md](PRD.md) for more details.

## Running Locally

### Prerequisites
- Java 17+
- Maven (or use the provided Maven Wrapper)

### Quick Start

1. **Build the Application**
   ```bash
   ./mvnw clean package
   ```

2. **Run the Server**
   ```bash
   java -jar target/edi-translator-pipeline-1.0.0-SNAPSHOT.jar
   ```

3. **Access the Dashboard**
   Open your browser and navigate to:
   ```
   http://localhost:8080 or http://localhost:8080/index.html
   ```

4. **Testing the Pipeline**
   - Upload any `.csv` or `.xml` file through the dashboard UI.
   - The Camel route will ingest the file, translate it, and the UI will reflect the egressed JSON payload in real-time.
   - Alternatively, drop a file directly into the `inbox/` directory and watch the UI update.

## Testing

Run the full suite of unit and integration tests covering route logic, Camel context advice, and MockMvc endpoints via:

```bash
./mvnw test
```

## Structure

- `src/main/java`: Core domain models, processors, Camel routes, and REST controllers.
- `src/main/resources/static`: Vanilla JS frontend dashboard for real-time monitoring.
- `k8s/`: Kubernetes deployment manifests.

## Author

**Abdennasser Bentaleb**
[abdennasserbentaleb@gmail.com](mailto:abdennasserbentaleb@gmail.com)
