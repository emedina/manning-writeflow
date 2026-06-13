package com.manning.writeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main bootstrap class for the WriteFlow Spring Boot application.
 *
 * @author Enrique Medina Montenegro
 */
@SpringBootApplication(proxyBeanMethods = false, scanBasePackages = "com.manning.writeflow")
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
