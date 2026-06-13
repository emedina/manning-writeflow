package com.manning.writeflow;

import com.emedina.hexagonal.application.command.CommandChecker;
import com.emedina.hexagonal.config.HexagonalArchitectureConfig;
import com.emedina.hexagonal.config.HexagonalArchitectureProperties;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Architecture test for the commands.
 *
 * @author Enrique Medina Montenegro
 */
@AnalyzeClasses(packages = "com.manning.writeflow.application.command")
@SpringBootTest(classes = { HexagonalArchitectureConfig.class, CommandArchitectureTest.TestConfig.class })
class CommandArchitectureTest {

    @Configuration
    static class TestConfig {

        @Bean
        public CommandChecker commandChecker(final HexagonalArchitectureProperties properties) {
            return new CommandChecker(properties);
        }

    }

    @Test
    void loadProperties() {
        // Forces the loading of the properties in the Spring context...
    }

    @ArchTest
    public static final ArchTests commandRules = ArchTests.in(CommandChecker.class);

}
