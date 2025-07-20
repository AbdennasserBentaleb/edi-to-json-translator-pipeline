package com.portfolio.edi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.portfolio.edi.config.JacksonConfig;
import com.portfolio.edi.controller.DummyRestController;
import com.portfolio.edi.processor.CsvToJsonProcessor;
import com.portfolio.edi.processor.XmlToJsonProcessor;
import com.portfolio.edi.routes.EdiTranslatorRoute;

/**
 * Main entry point for the Legacy EDI to JSON Translator Pipeline.
 * This 12-Factor App compliant microservice uses Apache Camel to poll legacy
 * flat files and XML, transform them to canonical JSON, and push to a REST
 * endpoint.
 */
@SpringBootApplication
@Import({
        JacksonConfig.class,
        CsvToJsonProcessor.class,
        XmlToJsonProcessor.class,
        EdiTranslatorRoute.class,
        DummyRestController.class
})
public class EdiTranslatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdiTranslatorApplication.class, args);
    }
}
