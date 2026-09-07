package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.Permission;
import com.IntraNet.Laucom.security.domain.port.PermissionRepositoryPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.PermissionJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.PermissionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class JpaPermissionRepositoryAdapter implements PermissionRepositoryPort {

    private final PermissionJpaRepository repository;

    public JpaPermissionRepositoryAdapter(PermissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> findByName(String name) {
        return repository.findById(name).map(PermissionPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return repository.findAll().stream().map(PermissionPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Permission save(Permission permission) {
        PermissionJpaEntity entity = repository.findById(permission.name())
                .orElseGet(() -> PermissionPersistenceMapper.toNewJpa(permission));
        entity.setDescription(permission.description());
        entity.setActive(permission.isActive());
        repository.save(entity);
        return permission;
    }
}
