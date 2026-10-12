package com.manning.writeflow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Keeps the relay independent of application packages.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@DisplayName("Outbox relay isolation")
class OutboxRelayArchitectureTest {

    private static final String RELAY_PACKAGE = "com.manning.writeflow.outboxrelay..";

    @Test
    @DisplayName("relay classes should not depend on application packages")
    void relayShouldNotDependOnApplicationPackages() {
        //given
        final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.manning.writeflow");

        //when
        final ArchRule rule = noClasses()
            .that().resideInAnyPackage(RELAY_PACKAGE)
            .should().dependOnClassesThat(resideInAnyPackage("com.manning.writeflow..")
                .and(resideOutsideOfPackage(RELAY_PACKAGE)))
            .because("the relay is outside the hexagon and uses an untyped workflow boundary");

        //then
        rule.check(importedClasses);
    }
}
