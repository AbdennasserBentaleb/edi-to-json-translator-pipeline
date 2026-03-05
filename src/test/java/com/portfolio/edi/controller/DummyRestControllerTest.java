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
}
