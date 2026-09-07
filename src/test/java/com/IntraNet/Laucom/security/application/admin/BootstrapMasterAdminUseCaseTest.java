package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.exception.MasterAdminBootstrapRequiredException;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.WellKnownRoles;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-014 (SPEC-AUTH-009) con Ports mockeados. */
@ExtendWith(MockitoExtension.class)
class BootstrapMasterAdminUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final String VALID_HASH = "$argon2id$v=19$m=16384,t=2,p=1$c29tZXNhbHQ$aGFzaHZhbHVl";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private AuditPort auditPort;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private Role masterAdminRole;

    @BeforeEach
    void setUp() {
        masterAdminRole = Role.createSystemRole(UUID.randomUUID(), WellKnownRoles.MASTER_ADMIN, "Administrador");
        lenient().when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.of(masterAdminRole));
    }

    private BootstrapMasterAdminUseCase useCase(String bootstrapHash) {
        return new BootstrapMasterAdminUseCase(userRepository, roleRepository, auditPort, clock,
                "admin", "Master Admin", "admin@example.com", bootstrapHash);
    }

    @Test
    void alreadyExistingAdmin_isIdempotent_doesNothing() {
        when(userRepository.existsActiveUserWithRole(masterAdminRole.id())).thenReturn(true);

        useCase(VALID_HASH).handle();

        verify(userRepository, never()).save(any());
        verify(auditPort, never()).record(any());
    }

    @Test
    void noAdmin_andNoBootstrapSecret_failsFast_RN07() {
        when(userRepository.existsActiveUserWithRole(masterAdminRole.id())).thenReturn(false);

        assertThatThrownBy(() -> useCase("").handle())
                .isInstanceOf(MasterAdminBootstrapRequiredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void noAdmin_andMalformedSecret_failsFast() {
        when(userRepository.existsActiveUserWithRole(masterAdminRole.id())).thenReturn(false);

        assertThatThrownBy(() -> useCase("not-an-argon2-hash").handle())
                .isInstanceOf(MasterAdminBootstrapRequiredException.class);
    }

    @Test
    void noAdmin_withValidSecret_createsAdminAccount_mustChangePasswordOnNextLogin_RN03() {
        when(userRepository.existsActiveUserWithRole(masterAdminRole.id())).thenReturn(false);

        useCase(VALID_HASH).handle();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();

        assertThat(admin.username()).isEqualTo("admin");
        assertThat(admin.credential()).hasValueSatisfying(
                c -> assertThat(c).isEqualTo(PasswordCredential.of(VALID_HASH, true)));
        assertThat(admin.findAssignment(masterAdminRole.id())).isPresent();
        verify(auditPort).record(any());
    }

    @Test
    void masterAdminRoleMissing_throwsIllegalState() {
        when(roleRepository.findByName(WellKnownRoles.MASTER_ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase(VALID_HASH).handle()).isInstanceOf(IllegalStateException.class);
    }
}
