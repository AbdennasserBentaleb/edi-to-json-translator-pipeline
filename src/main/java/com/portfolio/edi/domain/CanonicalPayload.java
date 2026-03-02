package com.portfolio.edi.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Standardized canonical JSON payload expected by modern REST downstream
 * systems.
 * Flat files or XML are parsed and transformed into this shape.
 * 
 * Using native Java Records as Lombok has limited support for early access Java
 * 25.
 */
public record CanonicalPayload(
        String transactionId,

        // Sender Information
        String senderId,
        String senderName,

        // Receiver Information
        String receiverId,

        // Core payload body
        String documentType, // E.g., INVOICE, PURCHASE_ORDER
        BigDecimal totalAmount,
        String currency, // E.g., EUR, USD

        // Processing Metadata
        LocalDateTime processingTimestamp) {
}
