package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.Role;
import com.IntraNet.Laucom.security.domain.port.RoleRepositoryPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PermissionJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.RoleJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.PermissionJpaRepository;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.RoleJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Antes de asociar un {@link Permission} a un {@link RoleJpaEntity}, lo resuelve a una
 * instancia JPA ya gestionada (existente o recién guardada) para que Hibernate no intente
 * reinsertar una Permission que ya existe — por eso esta resolución vive en el adapter y no en
 * {@link RolePersistenceMapper}, que no tiene acceso a los repositorios.
 */
@Component
public class JpaRoleRepositoryAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    public JpaRoleRepositoryAdapter(RoleJpaRepository roleJpaRepository, PermissionJpaRepository permissionJpaRepository) {
        this.roleJpaRepository = roleJpaRepository;
        this.permissionJpaRepository = permissionJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(UUID id) {
        return roleJpaRepository.findById(id.toString()).map(RolePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        return roleJpaRepository.findByName(name).map(RolePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleJpaRepository.findAll().stream().map(RolePersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Role save(Role role) {
        RoleJpaEntity entity = roleJpaRepository.findById(role.id().toString())
                .orElseGet(() -> new RoleJpaEntity(role.id().toString(), role.name(), role.description(),
                        role.isSystemRole(), role.isActive()));
        entity.setDescription(role.description());
        entity.setActive(role.isActive());

        Set<PermissionJpaEntity> managedPermissions = role.permissions().stream()
                .map(p -> permissionJpaRepository.findById(p.name())
                        .orElseGet(() -> permissionJpaRepository.save(PermissionPersistenceMapper.toNewJpa(p))))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        entity.getPermissions().clear();
        entity.getPermissions().addAll(managedPermissions);

        roleJpaRepository.save(entity);
        return role;
    }
}
