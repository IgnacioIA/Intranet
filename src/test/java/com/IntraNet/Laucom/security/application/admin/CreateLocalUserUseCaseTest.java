package com.IntraNet.Laucom.security.application.admin;

import com.IntraNet.Laucom.security.application.authorization.AuthorizationService;
import com.IntraNet.Laucom.security.application.exception.PasswordPolicyViolationException;
import com.IntraNet.Laucom.security.application.exception.UsernameAlreadyExistsException;
import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.domain.port.AuditPort;
import com.IntraNet.Laucom.security.domain.port.PasswordHasherPort;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

/** Cubre UC-AUTH-016 (SPEC-AUTH-010). */
@ExtendWith(MockitoExtension.class)
class CreateLocalUserUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private AuditPort auditPort;
    @Mock
    private AuthorizationService authorizationService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private CreateLocalUserUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(passwordHasher.hash(any())).thenReturn("hashed-value");
        AdminActionAuthorizer authorizer = new AdminActionAuthorizer(authorizationService, auditPort, clock);
        useCase = new CreateLocalUserUseCase(authorizer, userRepository, passwordHasher, auditPort, clock);
    }

    @Test
    void createsAUser_withAnAdminSuppliedPassword_noGeneratedPasswordReturned() {
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jperez")).thenReturn(Optional.empty());

        CreateLocalUserResult result = useCase.handle(ACTOR_ID,
                new CreateLocalUserCommand("jperez", "jperez@example.com", "Juan Perez",
                        "a-strong-password-123".toCharArray()),
                "corr-1");

        assertThat(result.user().username()).isEqualTo("jperez");
        assertThat(result.user().status()).isEqualTo(UserStatus.PENDING_ONBOARDING); // sin rol por defecto.
        assertThat(result.user().credential()).hasValueSatisfying(
                c -> assertThat(c).isEqualTo(PasswordCredential.of("hashed-value", true)));
        assertThat(result.generatedInitialPassword()).isNull();
    }

    @Test
    void createsAUser_withoutAnInitialPassword_generatesOne() {
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jperez")).thenReturn(Optional.empty());

        CreateLocalUserResult result = useCase.handle(ACTOR_ID,
                new CreateLocalUserCommand("jperez", "jperez@example.com", "Juan Perez", null), "corr-1");

        assertThat(result.generatedInitialPassword()).isNotBlank();
    }

    @Test
    void duplicateUsername_isRejected_INV_AUTH_001() {
        User existing = User.createLocal(UUID.randomUUID(), "jperez", "Otro", null,
                PasswordCredential.of("x", false), NOW);
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jperez"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateLocalUserCommand("jperez", "a@example.com", "A", "a-strong-password-123".toCharArray()),
                "corr-1"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void weakSuppliedPassword_isRejected() {
        when(userRepository.findByProviderAndUsername(IdentityProvider.LOCAL, "jperez")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.handle(ACTOR_ID,
                new CreateLocalUserCommand("jperez", "a@example.com", "A", "short".toCharArray()), "corr-1"))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }
}
