package com.manning.writeflow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Restricts Temporal SDK imports to the workflow driving adapter.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@DisplayName("Temporal dependency boundary")
class TemporalArchitectureTest {

    private static final String WORKFLOW_ADAPTER_PACKAGE = "com.manning.writeflow.workflow..";

    @Test
    @DisplayName("Temporal SDK imports should stay in the workflow adapter")
    void temporalImportsShouldStayInTheWorkflowAdapter() {
        //given
        final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.manning.writeflow");

        //when
        final ArchRule rule = noClasses()
            .that().resideOutsideOfPackage(WORKFLOW_ADAPTER_PACKAGE)
            .should().dependOnClassesThat().resideInAnyPackage("io.temporal..");

        //then
        rule.check(importedClasses);
    }
}
