package com.portfolio.edi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DummyRestControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyRestController()).build();
    }

    @Test
    @DisplayName("Should receive canonical JSON and return 200 OK")
    void testReceiveCanonicalJson() throws Exception {
        String canonicalJson = """
                {
                  "transactionId": "TXN-999",
                  "senderId": "S-123",
                  "senderName": "Test Sender",
                  "receiverId": "R-456",
                  "documentType": "INVOICE",
                  "totalAmount": 100.0,
                  "currency": "USD",
                  "processingTimestamp": "2026-03-05T12:00:00"
                }
                """;

        mockMvc.perform(post("/api/v1/canonical")
                .contentType(MediaType.APPLICATION_JSON)
                .content(canonicalJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully received and processed payload."));
    }

    @Test
    @DisplayName("Should handle highly concurrent payload submissions without race conditions")
    void testConcurrentPayloadSubmissions() throws Exception {
        int threadCount = 50;
        int submissionsPerThread = 20;
        int totalSubmissions = threadCount * submissionsPerThread;

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successField = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureField = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for start signal to maximize contention
                    for (int j = 0; j < submissionsPerThread; j++) {
                        String payload = "{\"transactionId\":\"" + java.util.UUID.randomUUID() + "\"}";
                        mockMvc.perform(post("/api/v1/canonical")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk());
                        successField.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureField.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads at once
        startLatch.countDown();

        // Wait for all threads to finish gracefully
        boolean processCompleted = doneLatch.await(15, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        org.junit.jupiter.api.Assertions.assertTrue(processCompleted, "Test timed out due to deadlocks or thread starvation");
        org.junit.jupiter.api.Assertions.assertEquals(0, failureField.get(), "Concurrency failures detected (race condition / exception)");
        org.junit.jupiter.api.Assertions.assertEquals(totalSubmissions, successField.get(), "Mismatch in successful MockMvc submissions");

        // Verify the application received exactly the amount of payloads sent
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/canonical")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseContent = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(responseContent);
        
        org.junit.jupiter.api.Assertions.assertEquals(totalSubmissions, rootNode.size(), "Total payloads stored should precisely match total submitted");
    }
}
