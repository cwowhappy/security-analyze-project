package org.cwowhappy.securityanalyze.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 架构规则测试，自动验证 DDD 分层依赖约束。
 */
class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("org.cwowhappy.securityanalyze");

    @Test
    void domainShouldNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..");
        rule.check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnInfrastructureOrInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..interfaces..");
        rule.check(importedClasses);
    }

    @Test
    void interfacesShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..interfaces..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    void domainClassesShouldNotUseSpringFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..");
        rule.check(importedClasses);
    }

    @Test
    void noCyclicDependenciesBetweenFeatures() {
        ArchRule rule = SlicesRuleDefinition.slices()
                .matching("org.cwowhappy.securityanalyze.(*)..")
                .should().beFreeOfCycles();
        rule.check(importedClasses);
    }

    @Test
    void controllerShouldResideInInterfacesLayer() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..interfaces.rest.controller..");
        rule.check(importedClasses);
    }

    @Test
    void repositoryImplementationShouldResideInInfrastructure() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().haveSimpleNameNotEndingWith("RepositoryTest")
                .should().resideInAnyPackage("..domain.repository..", "..infrastructure.persistence.repository..");
        rule.check(importedClasses);
    }
}
