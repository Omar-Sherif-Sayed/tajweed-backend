package io.github.omarsherifsayed.tajweed;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proves, on every build, the layering rules of {@code AGENTS.md} §9, so they cannot be broken by accident:
 * {@code general} never depends on a business module; controllers never reach repositories or entities; nothing
 * outside a module's service layer uses its {@code impl}; controllers and their names live where the convention says;
 * mappers are {@code final}.
 */
@AnalyzeClasses(packages = "io.github.omarsherifsayed.tajweed", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** What every module shares must not know about any module. */
    @ArchTest
    static final ArchRule generalDoesNotDependOnBusiness = noClasses()
            .that().resideInAPackage("..tajweed.general..")
            .should().dependOnClassesThat().resideInAPackage("..tajweed.business..")
            .allowEmptyShould(true);

    /** Controllers talk to services only — never to repositories or entities. */
    @ArchTest
    static final ArchRule controllersDoNotTouchTheDatabase = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..entity..")
            .allowEmptyShould(true);

    /** A service implementation is reached only through its interface. */
    @ArchTest
    static final ArchRule implementationsAreHidden = noClasses()
            .that().resideOutsideOfPackage("..service.impl..")
            .should().dependOnClassesThat().resideInAPackage("..service.impl..")
            .allowEmptyShould(true);

    /** REST controllers live in a {@code controller} package and are named {@code *Controller}. */
    @ArchTest
    static final ArchRule controllersAreWhereExpected = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..")
            .andShould().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    /** Entities live in an {@code entity} package. */
    @ArchTest
    static final ArchRule entitiesAreWhereExpected = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..entity..")
            .allowEmptyShould(true);

    /** Mappers are {@code final} classes of static methods ({@code AGENTS.md} §9). */
    @ArchTest
    static final ArchRule mappersAreFinal = classes()
            .that().resideInAPackage("..mapper..").and().haveSimpleNameEndingWith("Mapper")
            .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
            .allowEmptyShould(true);
}
