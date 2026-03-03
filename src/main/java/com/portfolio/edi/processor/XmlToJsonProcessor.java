package com.portfolio.edi.processor;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.portfolio.edi.domain.CanonicalPayload;
import com.portfolio.edi.domain.InvalidPayloadException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 12-Factor App Component: Transformer
 * Takes an irregular XML payload and converts it to canonical JSON shape.
 */
@Component
public class XmlToJsonProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(XmlToJsonProcessor.class);

    // 12FA: Constructor injection
    private final XmlMapper xmlMapper;

    public XmlToJsonProcessor(XmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);

        if (body == null || body.isBlank()) {
            throw new InvalidPayloadException("XML payload is empty or null.");
        }

        try {
            CanonicalPayload payload = xmlMapper.readValue(body, CanonicalPayload.class);

            // Enrich with current server time for Record
            if (payload.processingTimestamp() == null) {
                payload = new CanonicalPayload(
                        payload.transactionId(),
                        payload.senderId(),
                        payload.senderName(),
                        payload.receiverId(),
                        payload.documentType(),
                        payload.totalAmount(),
                        payload.currency(),
                        LocalDateTime.now());
            }

            exchange.getIn().setBody(payload);

        } catch (Exception e) {
            log.error("Failed to parse XML payload", e);
            throw new InvalidPayloadException("Malformed XML payload -> " + body, e);
        }
    }
}
