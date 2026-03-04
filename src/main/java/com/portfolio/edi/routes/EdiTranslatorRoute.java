package com.portfolio.edi.routes;

import com.portfolio.edi.domain.CanonicalPayload;
import com.portfolio.edi.processor.CsvToJsonProcessor;
import com.portfolio.edi.processor.XmlToJsonProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 12-Factor App compliant Camel Route.
 * Responsibilities:
 * 1. Poll the input directory for CSV or XML files.
 * 2. Handle parsing errors by routing them to a Dead Letter Channel (error
 * directory).
 * 3. Invoke specific Processors based on file extension to parse to
 * CanonicalPayload.
 * 4. Marshal CanonicalPayload to JSON.
 * 5. Push to REST endpoint.
 */
@Component
public class EdiTranslatorRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(EdiTranslatorRoute.class);

    @Value("${edi.input-dir}")
    private String inputDir;

    @Value("${edi.error-dir}")
    private String errorDir;

    @Value("${edi.rest-endpoint}")
    private String restEndpoint;

    private final CsvToJsonProcessor csvProcessor;
    private final XmlToJsonProcessor xmlProcessor;
    private final ObjectMapper objectMapper;

    public EdiTranslatorRoute(CsvToJsonProcessor csvProcessor, XmlToJsonProcessor xmlProcessor,
            ObjectMapper objectMapper) {
        this.csvProcessor = csvProcessor;
        this.xmlProcessor = xmlProcessor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() throws Exception {

        // 1. Dead Letter Channel setup
        // If an Exception is thrown anywhere in the route, move the original file here
        // and log.
        errorHandler(deadLetterChannel(errorDir)
                .useOriginalMessage()
                .log("Failed to process file ${file:name}. Moved to Dead Letter Queue.")
                .loggingLevel(LoggingLevel.ERROR));

        JacksonDataFormat jsonFormat = new JacksonDataFormat(CanonicalPayload.class);
        jsonFormat.setObjectMapper(objectMapper);

        // 2. Main Polling Route
        from(inputDir)
                .routeId("edi-to-json-pipeline")
                .log(LoggingLevel.INFO, "Picked up file: ${file:name}")
                // Content Based Router (EIP) based on the file extension
                .choice()
                .when(header("CamelFileName").endsWith(".csv"))
                .log(LoggingLevel.DEBUG, "Routing to CSV Processor")
                .process(csvProcessor)
                .when(header("CamelFileName").endsWith(".xml"))
                .log(LoggingLevel.DEBUG, "Routing to XML Processor")
                .process(xmlProcessor)
                .otherwise()
                .throwException(new com.portfolio.edi.domain.InvalidPayloadException("Unsupported file type received."))
                .end() // End Choice

                // 3. Transformation to JSON String
                .marshal(jsonFormat)
                .log(LoggingLevel.INFO, "Successfully transformed payload: ${body}")

                // 4. Egress: Send to REST API (using direct component for mocking, or HTTP
                // logic for real egress)
                // For now, let's route to a logical target we can mock/interact with in tests
                // or properties
                .to("direct:restEgress");

        // 5. REST Egress Sub-Route
        from("direct:restEgress")
                .routeId("rest-egress")
                .setHeader(org.apache.camel.Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(org.apache.camel.Exchange.CONTENT_TYPE, constant("application/json"))
                // In a real scenario we'd use .to(restEndpoint), here using http component or
                // mockable log
                .log(LoggingLevel.INFO, "Pushing to remote endpoint: " + restEndpoint)
                .to("log:rest-egress?level=INFO&showBody=true")
                // In integration tests, we can intercept this or wire to a mock HTTP server.
                // Using a stub definition for the HTTP call.
                .to(restEndpoint + "?throwExceptionOnFailure=false");
    }
}
