package com.IntraNet.Laucom.security.infrastructure.persistence;

import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.RefreshToken;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.infrastructure.persistence.adapter.JpaRefreshTokenRepositoryAdapter;
import com.IntraNet.Laucom.security.infrastructure.persistence.adapter.JpaUserRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 9 (Refresh Tokens) / docs/03-architecture/testing-strategy.md §1: persistencia probada
 * contra MySQL real vía Testcontainers, mismo patrón que {@code UserAuthorizationPersistenceIT}.
 *
 * <p><b>Nota de entorno:</b> requiere Docker disponible para levantar el contenedor MySQL. No se
 * pudo ejecutar en el sandbox de esta sesión (Docker no instalado); queda como el test permanente
 * del proyecto para CI / una máquina con Docker.</p>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaRefreshTokenRepositoryAdapter.class, JpaUserRepositoryAdapter.class})
class RefreshTokenPersistenceIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private JpaRefreshTokenRepositoryAdapter refreshTokenRepository;
    @Autowired
    private JpaUserRepositoryAdapter userRepository;

    private static final Instant NOW = Instant.parse("2026-09-07T00:00:00Z");

    /**
     * Reproduce contra MySQL real el escenario que causó
     * {@code SQLIntegrityConstraintViolationException} sobre {@code fk_refresh_tokens_replaced_by}
     * en {@code POST /auth/refresh}: el hijo de la rotación debe persistirse antes de que el
     * padre lo referencie por {@code replaced_by_token_id} (ver Javadoc de
     * {@code JpaRefreshTokenRepositoryAdapter#saveRotation}).
     */
    @Test
    void saveRotation_persistsChildBeforeReferencingItFromParent() {
        User user = User.createLocal(UUID.randomUUID(), "refresher", "Refresher", null,
                PasswordCredential.of("hash", false), NOW);
        userRepository.save(user);

        RefreshToken parent = RefreshToken.issueNewFamily(UUID.randomUUID(), user.id(), "hash-parent",
                NOW, NOW.plusSeconds(3600));
        refreshTokenRepository.save(parent);

        RefreshToken child = parent.rotate(UUID.randomUUID(), "hash-child", NOW.plusSeconds(1), NOW.plusSeconds(3601));

        refreshTokenRepository.saveRotation(parent, child);

        RefreshToken reloadedParent = refreshTokenRepository.findByTokenHash("hash-parent").orElseThrow();
        assertThat(reloadedParent.revokedAt()).isPresent();
        assertThat(reloadedParent.replacedByTokenId()).contains(child.id());

        RefreshToken reloadedChild = refreshTokenRepository.findByTokenHash("hash-child").orElseThrow();
        assertThat(reloadedChild.revokedAt()).isEmpty();
    }
}
