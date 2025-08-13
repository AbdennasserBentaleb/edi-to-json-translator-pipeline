package com.portfolio.edi.routes;

import com.portfolio.edi.EdiTranslatorApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.portfolio.edi.processor.CsvToJsonProcessor;
import com.portfolio.edi.processor.XmlToJsonProcessor;
import com.portfolio.edi.config.JacksonConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@CamelSpringBootTest
@SpringBootTest(classes = EdiTranslatorApplication.class)
@UseAdviceWith // Tell Camel not to start the context automatically, we will advise routes
               // first
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "edi.input-uri=direct:test-inbox",
        "edi.error-uri=mock:test-error",
        "edi.rest-endpoint=mock:test-egress"
})
class EdiTranslatorRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Produce("direct:test-inbox")
    private ProducerTemplate producerTemplate;

    @EndpointInject("mock:test-error")
    private MockEndpoint errorEndpoint;

    @EndpointInject("mock:test-egress")
    private MockEndpoint egressEndpoint;

    @BeforeEach
    void setUp() throws Exception {
        if (!camelContext.isStarted()) {
            // Intercept routes to inject mock endpoints
            AdviceWith.adviceWith(camelContext, "edi-to-json-pipeline", routeBuilder -> {
                // Replace the 'to("direct:restEgress")' step with our mock egress
                routeBuilder.weaveByToUri("direct:restEgress").replace().to("mock:test-egress");
            });
            camelContext.start();
        }

        // Reset mocks before each test
        errorEndpoint.reset();
        egressEndpoint.reset();
    }

    @Test
    @DisplayName("Should successfully parse valid CSV and route to REST egress endpoint")
    void testValidCsvRouting() throws Exception {
        // Arrange
        String validCsv = "TXN-001,S-001,Acme Corp,R-999,INVOICE,150.50,EUR";
        egressEndpoint.expectedMessageCount(1);
        errorEndpoint.expectedMessageCount(0);

        // Act
        producerTemplate.sendBodyAndHeader(validCsv, "CamelFileName", "invoice.csv");

        // Assert
        egressEndpoint.assertIsSatisfied();
        errorEndpoint.assertIsSatisfied();

        // Optional: Verify the JSON structure in the mock endpoint
        String jsonPayload = egressEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        org.junit.jupiter.api.Assertions.assertTrue(jsonPayload.contains("\"transactionId\":\"TXN-001\""));
        org.junit.jupiter.api.Assertions.assertTrue(jsonPayload.contains("\"currency\":\"EUR\""));
        org.junit.jupiter.api.Assertions.assertTrue(jsonPayload.contains("\"totalAmount\":150.5"));
    }

    @Test
    @DisplayName("Should trigger Dead Letter Channel when parsing malformed CSV")
    void testMalformedCsvRoutingToDeadLetter() throws Exception {
        // Arrange
        // Missing columns, should throw index out of bounds or our custom exception
        String malformedCsv = "TXN-002,S-001";
        egressEndpoint.expectedMessageCount(0);
        errorEndpoint.expectedMessageCount(1);

        // Assert the original body is preserved in the DLC
        errorEndpoint.expectedBodiesReceived(malformedCsv);

        // Act
        producerTemplate.sendBodyAndHeader(malformedCsv, "CamelFileName", "bad_invoice.csv");

        // Assert
        egressEndpoint.assertIsSatisfied();
        errorEndpoint.assertIsSatisfied();
    }

    @Test
    @DisplayName("Should successfully parse valid XML and route to REST egress endpoint")
    void testValidXmlRouting() throws Exception {
        // Arrange
        String validXml = """
                <CanonicalPayload>
                    <transactionId>TXN-XML-001</transactionId>
                    <senderId>S-002</senderId>
                    <senderName>Globex Inc</senderName>
                    <receiverId>R-888</receiverId>
                    <documentType>PURCHASE_ORDER</documentType>
                    <totalAmount>999.99</totalAmount>
                    <currency>USD</currency>
                </CanonicalPayload>
                """;
        egressEndpoint.expectedMessageCount(1);
        errorEndpoint.expectedMessageCount(0);

        // Act
        producerTemplate.sendBodyAndHeader(validXml, "CamelFileName", "order.xml");

        // Assert
        egressEndpoint.assertIsSatisfied();
        errorEndpoint.assertIsSatisfied();

        String jsonPayload = egressEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        org.junit.jupiter.api.Assertions.assertTrue(jsonPayload.contains("\"transactionId\":\"TXN-XML-001\""));
    }
}
