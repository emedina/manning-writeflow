package com.manning.writeflow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Project-specific architecture rule that guards the aggregate reconstitution path.
 *
 * <p>{@code Article.reconstitute(...)} has to be {@code public} because the persistence mapper
 * that rebuilds a stored aggregate lives in a different Maven module than the domain, so neither
 * a {@code private} nor a package-private factory can reach it. Being public, however, it would
 * otherwise let any application code fabricate an {@link com.manning.writeflow.domain.entities.Article}
 * in an arbitrary state &mdash; bypassing the always-valid creation path
 * ({@code validateThenCreate}) and the {@code publish(...)} state machine.</p>
 *
 * <p>Rather than weaken the type system, we treat this as an architectural boundary and enforce it
 * the same way the rest of the hexagon is enforced: as an executable specification. Reconstitution
 * is a persistence concern, so the only code allowed to invoke it is the persistence adapter
 * ({@code com.manning.writeflow.repositories..}). Any other caller &mdash; a handler, another
 * adapter, a controller &mdash; fails this test.</p>
 *
 * <p>This complements the generic checkers shipped by the Architecture Kernel
 * ({@code archunit-hexagonal}): those rules are reused as-is via {@code ArchTests.in(...)} in the
 * sibling test classes, while this class shows that the same ArchUnit foundation can be extended
 * with rules specific to WriteFlow's own design decisions.</p>
 *
 * @author Enrique Medina Montenegro
 */
@DisplayName("Reconstitution access rule")
class ReconstitutionAccessArchitectureTest {

    private static final String DOMAIN_ENTITIES_PACKAGE = "com.manning.writeflow.domain.entities";
    private static final String PERSISTENCE_ADAPTER_PACKAGE = "com.manning.writeflow.repositories..";

    @Test
    @DisplayName("Article.reconstitute() may only be called from the persistence adapter")
    void reconstituteMustOnlyBeCalledFromThePersistenceAdapter() {
        final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.manning.writeflow");

        final ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN_ENTITIES_PACKAGE)
            .and().haveName("reconstitute")
            .should().onlyBeCalled().byClassesThat().resideInAnyPackage(PERSISTENCE_ADAPTER_PACKAGE);

        rule.check(importedClasses);
    }

}
