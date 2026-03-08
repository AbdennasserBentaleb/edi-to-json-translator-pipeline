# Product Requirements Document (PRD)

## Project Overview
The EDI to JSON Translator Pipeline is a microservice designed to modernize legacy B2B integration patterns. It ingests legacy Electronic Data Interchange (EDI) file formats, specifically CSV and XML, validates the content, translates it into a canonical JSON representation, and forwards the payload to an egress REST API.

## Problem Statement
Many enterprise systems still rely on dropping legacy file formats onto FTP servers or shared network drives. Modern downstream systems require structured JSON data via HTTP. There is a need for a reliable, idempotent, and error-tolerant pipeline to bridge this gap without requiring changes to the legacy producer systems.

## Target Audience
- **Integration Engineers:** Need a reliable mechanism to connect legacy systems to modern APIs.
- **Operations Teams:** Require visibility into the ingestion process, including dead-lettered files and successful translations.
- **Backend Developers:** Need a canonical data format to simplify downstream consumption logic.

## Functional Requirements
1. **File Ingestion:** The system must poll a configured `inbox` directory for new `.csv` and `.xml` files.
2. **Format Detection:** The system must route files to the correct processor based on their extension.
3. **Translation (CSV):** Parse comma-separated values into a `CanonicalPayload` object.
4. **Translation (XML):** Parse hierarchical XML into a `CanonicalPayload` object.
5. **Output Generation:** Marshal the `CanonicalPayload` into a standard JSON string.
6. **REST Egress:** POST the resulting JSON string to a configured HTTP endpoint.
7. **Error Handling:** Invalid files or processing exceptions must result in the original file being moved to an `error` directory (Dead Letter Channel).
8. **Dashboard:** A web-based UI to monitor the real-time processing of payloads and allow manual file uploads for testing.

## Non-Functional Requirements
- **Idempotency:** A file successfully processed must be deleted or moved from the inbox to prevent duplicate processing.
- **Robustness:** Processing one bad file must not crash the pipeline or stop subsequent files from being processed.
- **Observability:** Key events (file picked up, translated, egressed, failed) must be logged clearly.
- **12-Factor Compliance:** Configuration (directories, endpoints) must be provided via environment variables or properties.

## Success Metrics
- 100% of valid CSV/XML files correctly translated and posted to the egress endpoint.
- 0% data loss during unexpected shutdowns (files remain in inbox if not fully processed).
- Zero downtime deployments supported via Kubernetes manifests.
