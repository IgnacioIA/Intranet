package com.IntraNet.Laucom.security.domain.model;

import com.IntraNet.Laucom.security.domain.exception.InvalidUserStateTransitionException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate Root de identidad. Representa un usuario {@code LOCAL} o {@code ACTIVE_DIRECTORY}.
 * Ver entities.md "User", SPEC-AUTH-001, SPEC-AUTH-006, SPEC-AUTH-007, SPEC-AUTH-009,
 * SPEC-AUTH-010.
 *
 * <p>Efectos colaterales que exceden este agregado (revocar {@code RefreshToken} al cambiar de
 * estado — REQ-AUTH-011 aplicado automáticamente; verificar la continuidad de
 * {@code MASTER_ADMIN} — INV-AUTH-013; resolver el {@code Role ONBOARDING_USER} concreto)
 * se orquestan en la capa de aplicación, no en esta clase.</p>
 */
public final class User {

    private final UUID id;
    private final IdentityProvider provider;
    private final String externalId;
    private String username;
    private String displayName;
    private String email;
    private UserStatus status;
    private PasswordCredential credential;
    private final Map<UUID, UserRoleAssignment> roles = new LinkedHashMap<>();
    private final Instant createdAt;
    private Instant lastLoginAt;
    private Instant lastAdSyncAt;
    private int failedLoginAttempts;
    private Instant lockedUntil;

    private User(UUID id, IdentityProvider provider, String externalId, String username,
                 String displayName, String email, UserStatus status, PasswordCredential credential,
                 Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.externalId = externalId;
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = displayName;
        this.email = email;
        this.status = Objects.requireNonNull(status, "status");
        this.credential = credential;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** REQ-AUTH-027, UC-AUTH-016: alta LOCAL. Sin roles; INV-AUTH-004 deja al usuario en PENDING_ONBOARDING. */
    public static User createLocal(UUID id, String username, String displayName, String email,
                                    PasswordCredential credential, Instant now) {
        return new User(id, IdentityProvider.LOCAL, null, username, displayName, email,
                UserStatus.PENDING_ONBOARDING, credential, now);
    }

    /** SPEC-AUTH-001 UC-AUTH-003: alta de Shadow Identity. INV-AUTH-003: sin credential. */
    public static User provisionFromDirectory(UUID id, String externalId, String username,
                                               String displayName, String email, Instant now) {
        Objects.requireNonNull(externalId, "externalId");
        return new User(id, IdentityProvider.ACTIVE_DIRECTORY, externalId, username, displayName,
                email, UserStatus.PENDING_ONBOARDING, null, now);
    }

    /** Reconstrucción desde persistencia (Fase 2). No reaplica invariantes de alta. */
    public static User reconstitute(UUID id, IdentityProvider provider, String externalId, String username,
                                     String displayName, String email, UserStatus status,
                                     PasswordCredential credential, Set<UserRoleAssignment> roleAssignments,
                                     Instant createdAt, Instant lastLoginAt, Instant lastAdSyncAt,
                                     int failedLoginAttempts, Instant lockedUntil) {
        User user = new User(id, provider, externalId, username, displayName, email, status, credential, createdAt);
        roleAssignments.forEach(a -> user.roles.put(a.roleId(), a));
        user.lastLoginAt = lastLoginAt;
        user.lastAdSyncAt = lastAdSyncAt;
        user.failedLoginAttempts = failedLoginAttempts;
        user.lockedUntil = lockedUntil;
        return user;
    }

    // --- Consultas ---

    public boolean isLocal() {
        return provider == IdentityProvider.LOCAL;
    }

    public boolean isActiveDirectory() {
        return provider == IdentityProvider.ACTIVE_DIRECTORY;
    }

    /** RN-01 SPEC-AUTH-001: solo ACTIVE o PENDING_ONBOARDING pueden autenticarse. */
    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING_ONBOARDING;
    }

    public Optional<UserRoleAssignment> findAssignment(UUID roleId) {
        return Optional.ofNullable(roles.get(roleId));
    }

    // --- Gestión de contraseña LOCAL (SPEC-AUTH-007) ---

    public void changeLocalPassword(PasswordCredential newCredential) {
        if (!isLocal()) {
            throw new IllegalStateException(
                    "La contraseña de un usuario ACTIVE_DIRECTORY no es gestionada por la aplicación (REQ-AUTH-012)");
        }
        this.credential = Objects.requireNonNull(newCredential, "newCredential");
    }

    // --- Intentos fallidos (RN-07 SPEC-AUTH-001, RN-05 SPEC-AUTH-009) ---

    public void recordFailedLoginAttempt(int maxAttempts, Instant now, Duration cooldown) {
        this.failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts && status == UserStatus.ACTIVE) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = now.plus(cooldown);
        }
    }

    public void recordSuccessfulLogin(Instant now) {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = now;
    }

    public boolean isCooldownElapsed(Instant now) {
        return lockedUntil == null || !now.isBefore(lockedUntil);
    }

    /**
     * RN-05 SPEC-AUTH-009 (Decision Ledger 2026-09-06): el bloqueo por intentos fallidos nunca
     * debe ser permanente para la cuenta {@code MASTER_ADMIN} — quién debe invocar este método
     * (y verificar que el usuario realmente posee ese Role) es responsabilidad de la capa de
     * aplicación ({@code AuthenticateLocalUserUseCase}), no de este agregado. Sin efecto si el
     * usuario no está {@code LOCKED} o si el cooldown todavía no expiró (idempotente/seguro de
     * invocar siempre).
     */
    public void autoUnlockIfCooldownElapsed(Instant now) {
        if (status == UserStatus.LOCKED && isCooldownElapsed(now)) {
            this.lockedUntil = null;
            this.failedLoginAttempts = 0;
            this.status = roles.isEmpty() ? UserStatus.PENDING_ONBOARDING : UserStatus.ACTIVE;
        }
    }

    // --- Asignación de roles (REQ-AUTH-031, REQ-AUTH-032, INV-AUTH-005, INV-AUTH-015) ---

    /**
     * Otorga o actualiza una asignación. Si ya existía {@code DERIVED_FROM_AD} y la nueva es
     * {@code GRANTED_EXPLICITLY}, hace upgrade en el mismo registro (INV-AUTH-015). Una
     * asignación {@code DERIVED_FROM_AD} nunca sobrescribe una {@code GRANTED_EXPLICITLY}
     * existente (INV-AUTH-005). Misma procedencia repetida: idempotente (RN-12 SPEC-AUTH-010).
     */
    public void assignRole(UserRoleAssignment assignment, Instant now) {
        Objects.requireNonNull(assignment, "assignment");
        UserRoleAssignment existing = roles.get(assignment.roleId());
        if (existing == null) {
            roles.put(assignment.roleId(), assignment);
            recomputeOnboardingStatus();
            return;
        }
        if (existing.provenance() == RoleProvenance.GRANTED_EXPLICITLY
                && assignment.provenance() == RoleProvenance.DERIVED_FROM_AD) {
            return; // INV-AUTH-005
        }
        if (existing.provenance() == RoleProvenance.DERIVED_FROM_AD
                && assignment.provenance() == RoleProvenance.GRANTED_EXPLICITLY) {
            roles.put(assignment.roleId(), existing.upgradeToExplicit(now)); // INV-AUTH-015
            recomputeOnboardingStatus();
        }
        // misma procedencia en ambos: idempotente, sin cambios.
    }

    /** REQ-AUTH-032, RN-15 SPEC-AUTH-010: revocar el último Role deja al usuario en PENDING_ONBOARDING. */
    public void revokeRole(UUID roleId) {
        if (roles.remove(roleId) == null) {
            throw new IllegalArgumentException("El usuario no posee el Role indicado");
        }
        recomputeOnboardingStatus();
    }

    /**
     * SPEC-AUTH-001 UC-AUTH-004: reconcilia las asignaciones {@code DERIVED_FROM_AD} contra el
     * conjunto vigente de grupos AD ya evaluado por la capa de aplicación. Las asignaciones
     * {@code GRANTED_EXPLICITLY} nunca se modifican (INV-AUTH-005).
     */
    public void reconcileDerivedRoles(Set<UserRoleAssignment> currentDerivedAssignments, Instant now) {
        currentDerivedAssignments.forEach(a -> {
            if (a.provenance() != RoleProvenance.DERIVED_FROM_AD) {
                throw new IllegalArgumentException("currentDerivedAssignments solo admite DERIVED_FROM_AD");
            }
        });
        Set<UUID> stillValid = currentDerivedAssignments.stream()
                .map(UserRoleAssignment::roleId)
                .collect(Collectors.toSet());
        roles.entrySet().removeIf(e -> e.getValue().provenance() == RoleProvenance.DERIVED_FROM_AD
                && !stillValid.contains(e.getKey()));
        for (UserRoleAssignment a : currentDerivedAssignments) {
            roles.putIfAbsent(a.roleId(), a);
        }
        this.lastAdSyncAt = now;
        recomputeOnboardingStatus();
    }

    /** INV-AUTH-004: sin roles vigentes → PENDING_ONBOARDING; con al menos uno → ACTIVE. */
    private void recomputeOnboardingStatus() {
        if (status != UserStatus.ACTIVE && status != UserStatus.PENDING_ONBOARDING) {
            return; // no se recalcula sobre LOCKED/DISABLED/DEPROVISIONED.
        }
        this.status = roles.isEmpty() ? UserStatus.PENDING_ONBOARDING : UserStatus.ACTIVE;
    }

    // --- Transiciones administrativas (SPEC-AUTH-010 UC-AUTH-018, 02-domain/transitions.md) ---
    // NOTA: la verificación de INV-AUTH-013 (continuidad de MASTER_ADMIN) requiere una
    // consulta al repositorio y se realiza en la capa de aplicación ANTES de invocar estos
    // métodos, no dentro de ellos.

    public void enable(Instant now) {
        if (status != UserStatus.DISABLED) {
            throw new InvalidUserStateTransitionException(status, "enable");
        }
        this.status = roles.isEmpty() ? UserStatus.PENDING_ONBOARDING : UserStatus.ACTIVE;
    }

    public void disable(Instant now) {
        if (status == UserStatus.DISABLED || status == UserStatus.DEPROVISIONED) {
            throw new InvalidUserStateTransitionException(status, "disable");
        }
        this.status = UserStatus.DISABLED;
    }

    public void lock(Instant now) {
        if (status != UserStatus.ACTIVE && status != UserStatus.PENDING_ONBOARDING) {
            throw new InvalidUserStateTransitionException(status, "lock");
        }
        this.status = UserStatus.LOCKED;
        this.lockedUntil = null; // bloqueo administrativo manual: sin cooldown automático asociado.
    }

    public void unlock(Instant now) {
        if (status != UserStatus.LOCKED) {
            throw new InvalidUserStateTransitionException(status, "unlock");
        }
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
        this.status = roles.isEmpty() ? UserStatus.PENDING_ONBOARDING : UserStatus.ACTIVE;
    }

    public void deprovision(Instant now) {
        if (status == UserStatus.DEPROVISIONED) {
            throw new InvalidUserStateTransitionException(status, "deprovision");
        }
        this.status = UserStatus.DEPROVISIONED;
    }

    // --- Identidad (RN-02 SPEC-AUTH-010) ---

    public void syncDirectoryAttributes(String username, String displayName, String email) {
        if (!isActiveDirectory()) {
            throw new IllegalStateException("Solo se sincronizan atributos de usuarios ACTIVE_DIRECTORY");
        }
        this.username = username;
        this.displayName = displayName;
        this.email = email;
    }

    public void updateLocalIdentity(String email, String displayName) {
        if (!isLocal()) {
            throw new IllegalStateException("RN-02 SPEC-AUTH-010: solo se edita identidad de usuarios LOCAL");
        }
        this.email = email;
        this.displayName = displayName;
    }

    // --- Accesores ---

    public UUID id() {
        return id;
    }

    public IdentityProvider provider() {
        return provider;
    }

    public Optional<String> externalId() {
        return Optional.ofNullable(externalId);
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    public UserStatus status() {
        return status;
    }

    public Optional<PasswordCredential> credential() {
        return Optional.ofNullable(credential);
    }

    public Set<UserRoleAssignment> roles() {
        return Set.copyOf(roles.values());
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> lastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
    }

    public Optional<Instant> lastAdSyncAt() {
        return Optional.ofNullable(lastAdSyncAt);
    }

    public int failedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Optional<Instant> lockedUntil() {
        return Optional.ofNullable(lockedUntil);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", provider=" + provider + ", username=" + username + ", status=" + status + "]";
    }
}
