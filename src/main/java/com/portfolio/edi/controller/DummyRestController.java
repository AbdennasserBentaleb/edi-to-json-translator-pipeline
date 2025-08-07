package com.portfolio.edi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 12-Factor App Component: Dummy REST Endpoint
 * Acts as the egress target for canonical JSON payloads from the Camel Route.
 */
@RestController
@RequestMapping("/api/v1")
public class DummyRestController {

    private static final Logger log = LoggerFactory.getLogger(DummyRestController.class);

    // In-memory store for demonstrations via the frontend
    private final List<String> canonicalPayloads = Collections.synchronizedList(new ArrayList<>());

    @PostMapping("/canonical")
    public ResponseEntity<String> receiveCanonicalJson(@RequestBody String jsonPayload) {
        log.info("Received Canonical JSON at dummy endpoint: {}", jsonPayload);

        // Add to the front of the list for the UI
        canonicalPayloads.add(0, jsonPayload);

        return ResponseEntity.ok("Successfully received and processed payload.");
    }

    @GetMapping("/canonical")
    public ResponseEntity<List<String>> getCanonicalPayloads() {
        return ResponseEntity.ok(canonicalPayloads);
    }

    @DeleteMapping("/canonical")
    public ResponseEntity<Void> clearCanonicalPayloads() {
        canonicalPayloads.clear();
        return ResponseEntity.noContent().build();
    }
}
