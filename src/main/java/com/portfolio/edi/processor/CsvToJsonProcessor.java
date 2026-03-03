package com.portfolio.edi.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.edi.domain.CanonicalPayload;
import com.portfolio.edi.domain.InvalidPayloadException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 12-Factor App Component: Transformer
 * Takes an irregular CSV (e.g., ID,Name,DocType,Total,Curr) and parses it
 * into the structured CanonicalPayload object.
 */
@Component
public class CsvToJsonProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(CsvToJsonProcessor.class);

    // 12FA: Constructor injection used.
    private final ObjectMapper objectMapper;

    public CsvToJsonProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);

        if (body == null || body.isBlank()) {
            throw new InvalidPayloadException("CSV payload is empty or null.");
        }

        try {
            // Very naive split for demonstration:
            // Expected format:
            // TransactionId,SenderId,SenderName,ReceiverId,DocType,Amount,Currency
            String[] parts = body.split(",");

            if (parts.length < 7) {
                throw new InvalidPayloadException("Malformed CSV payload. Expected 7 columns, got: " + parts.length);
            }

            CanonicalPayload payload = new CanonicalPayload(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim(),
                    parts[3].trim(),
                    parts[4].trim(),
                    new BigDecimal(parts[5].trim()),
                    parts[6].trim(),
                    LocalDateTime.now());

            // Set the parsed object back into the exchange body for the next route step
            exchange.getIn().setBody(payload);

        } catch (NumberFormatException e) {
            log.error("Failed to parse number in CSV", e);
            throw new InvalidPayloadException("Invalid numeric format in CSV -> " + body, e);
        } catch (Exception e) {
            log.error("Failed to parse CSV payload", e);
            throw new InvalidPayloadException("General failure in parsing CSV -> " + body, e);
        }
    }
}
