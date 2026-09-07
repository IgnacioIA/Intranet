package com.IntraNet.Laucom.security.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica los límites arquitectónicos de REQ-AUTH-021 / ADR-001: el dominio del módulo de
 * seguridad no depende de Spring, JPA/Hibernate, JWT ni de una implementación concreta de
 * directorio (AD/LDAP); la aplicación no depende de adapters concretos de infraestructura.
 */
class SecurityModuleArchitectureTest {

    private static final String DOMAIN_PACKAGE = "com.IntraNet.Laucom.security.domain..";
    private static final String APPLICATION_PACKAGE = "com.IntraNet.Laucom.security.application..";
    private static final String INFRASTRUCTURE_PACKAGE = "com.IntraNet.Laucom.security.infrastructure..";

    private static JavaClasses securityModuleClasses;

    @BeforeAll
    static void importClasses() {
        securityModuleClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.IntraNet.Laucom.security");
    }

    @Test
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
        rule.check(securityModuleClasses);
    }

    @Test
    void domainMustNotDependOnPersistenceFrameworks() {
        ArchRule rule = noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..", "javax.persistence..", "org.hibernate..");
        rule.check(securityModuleClasses);
    }

    @Test
    void domainMustNotDependOnJwtLibraries() {
        ArchRule rule = noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("io.jsonwebtoken..", "com.nimbusds..");
        rule.check(securityModuleClasses);
    }

    @Test
    void domainMustNotDependOnDirectoryLibraries() {
        ArchRule rule = noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("javax.naming..", "com.unboundid..");
        rule.check(securityModuleClasses);
    }

    @Test
    void domainMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE_PACKAGE);
        rule.check(securityModuleClasses);
    }

    @Test
    void applicationMustNotDependOnInfrastructure() {
        // Desde Fase 4 el paquete "application" ya tiene clases reales: la regla verifica de
        // verdad (ya no necesita allowEmptyShould, que era un puente temporal de Fase 0/1).
        ArchRule rule = noClasses().that().resideInAPackage(APPLICATION_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE_PACKAGE);
        rule.check(securityModuleClasses);
    }

    @Test
    void roleAndPermissionJpaRepositoriesMustNotExposePhysicalDeletion_INV_AUTH_014() {
        // Fase 18 (Soft Deactivation, verificación): RoleJpaRepository/PermissionJpaRepository
        // extienden el marcador Repository (sin métodos), no JpaRepository/CrudRepository, que
        // traen delete/deleteById/deleteAll por herencia. Esta regla evita una regresión futura
        // (ej. alguien los cambia de vuelta a JpaRepository "por comodidad").
        ArchRule rule = noClasses()
                .that().haveNameMatching(".*\\.(RoleJpaRepository|PermissionJpaRepository)")
                .should().beAssignableTo("org.springframework.data.repository.CrudRepository");
        rule.check(securityModuleClasses);
    }

    @Test
    void adminUseCasesMustEnforceAuthorization() {
        // Fase 22 (Security Review): Spring Security en el borde (SecurityFilterChainConfig) solo
        // exige "authenticated()" — ADR-014 deja deliberadamente sin implementar la derivación de
        // GrantedAuthority a partir de Permission. Esto significa que el RBAC real de
        // SPEC-AUTH-010 depende POR COMPLETO de que cada Use Case administrativo invoque
        // AdminActionAuthorizer.require(...) (RN-16). Esta regla es la única red de seguridad
        // estructural contra un futuro Use Case administrativo agregado sin ese control.
        // BootstrapMasterAdminUseCase es la única excepción legítima: corre durante el arranque
        // de la aplicación, sin ningún actor autenticado (SPEC-AUTH-009, UC-AUTH-014).
        ArchRule rule = classes()
                .that().resideInAPackage("com.IntraNet.Laucom.security.application.admin..")
                .and().haveSimpleNameEndingWith("UseCase")
                .and().haveSimpleNameNotEndingWith("BootstrapMasterAdminUseCase")
                .should().dependOnClassesThat().haveSimpleName("AdminActionAuthorizer");
        rule.check(securityModuleClasses);
    }
}
