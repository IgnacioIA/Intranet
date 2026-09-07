package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.port.PageResult;
import com.IntraNet.Laucom.security.domain.port.UserRepositoryPort;
import com.IntraNet.Laucom.security.domain.port.UserSearchCriteria;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserRoleAssignmentJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.UserJpaRepository;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.UserRoleAssignmentJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de infraestructura para {@link UserRepositoryPort}. El dominio no conoce esta clase
 * ni JPA/Hibernate (ADR-001, REQ-AUTH-021 — ver SecurityModuleArchitectureTest).
 *
 * <p>Persiste {@code User} y sus {@code UserRoleAssignment} en dos pasos dentro de la misma
 * transacción: guarda la fila de usuario y luego reemplaza sus asignaciones (borrar todas,
 * reinsertar las vigentes). Es correcto porque la cantidad de roles por usuario es pequeña y
 * evita la complejidad de un {@code @OneToMany} JPA con clave compuesta.</p>
 */
@Component
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final UserRoleAssignmentJpaRepository assignmentJpaRepository;

    public JpaUserRepositoryAdapter(UserJpaRepository userJpaRepository,
                                     UserRoleAssignmentJpaRepository assignmentJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.assignmentJpaRepository = assignmentJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id.toString()).map(this::toDomainWithAssignments);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByProviderAndUsername(IdentityProvider provider, String username) {
        return userJpaRepository.findByProviderAndUsername(provider.name(), username)
                .map(this::toDomainWithAssignments);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByExternalId(String externalId) {
        return userJpaRepository.findByExternalId(externalId).map(this::toDomainWithAssignments);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByProviderAndEmail(IdentityProvider provider, String email) {
        return userJpaRepository.findByProviderAndEmail(provider.name(), email).map(this::toDomainWithAssignments);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveUserWithRole(UUID roleId) {
        return assignmentJpaRepository.existsActiveUserWithRole(roleId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveUsersWithRole(UUID roleId) {
        return assignmentJpaRepository.countActiveUsersWithRole(roleId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> search(UserSearchCriteria criteria) {
        String status = criteria.status() == null ? null : criteria.status().name();
        String provider = criteria.provider() == null ? null : criteria.provider().name();
        String search = (criteria.searchText() == null || criteria.searchText().isBlank())
                ? null : criteria.searchText();

        Page<UserJpaEntity> page = userJpaRepository.search(status, provider, search,
                PageRequest.of(criteria.page(), criteria.size()));

        List<User> users = page.getContent().stream().map(this::toDomainWithAssignments).toList();
        return new PageResult<>(users, criteria.page(), criteria.size(), page.getTotalElements());
    }

    @Override
    @Transactional
    public User save(User user) {
        // saveAndFlush (no save): las violaciones de INV-AUTH-001/INV-AUTH-002 deben fallar de
        // inmediato en esta operación, no quedar diferidas hasta un flush posterior arbitrario.
        UserJpaEntity entity = UserPersistenceMapper.toNewJpa(user);
        userJpaRepository.saveAndFlush(entity);
        assignmentJpaRepository.deleteByIdUserId(user.id().toString());
        assignmentJpaRepository.flush();
        List<UserRoleAssignmentJpaEntity> assignments = UserPersistenceMapper.toAssignmentJpa(user);
        assignmentJpaRepository.saveAll(assignments);
        assignmentJpaRepository.flush();
        return user;
    }

    private User toDomainWithAssignments(UserJpaEntity entity) {
        List<UserRoleAssignmentJpaEntity> assignments = assignmentJpaRepository.findByIdUserId(entity.getId());
        return UserPersistenceMapper.toDomain(entity, assignments);
    }
}
