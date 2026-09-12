package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.application.exception.AdGroupMappingAlreadyExistsException;
import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.AdGroupRoleMappingJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.AdGroupRoleMappingPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.AdGroupRoleMappingJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JpaAdGroupRoleMappingRepositoryAdapter implements AdGroupRoleMappingRepositoryPort {

    private final AdGroupRoleMappingJpaRepository jpaRepository;

    public JpaAdGroupRoleMappingRepositoryAdapter(AdGroupRoleMappingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AdGroupRoleMapping> findByAdGroupIdentifierIn(Set<String> adGroupIdentifiers) {
        return jpaRepository.findByAdGroupIdentifierIn(adGroupIdentifiers).stream()
                .map(AdGroupRoleMappingPersistenceMapper::toDomain)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdGroupRoleMapping> findById(UUID id) {
        return jpaRepository.findById(id.toString()).map(AdGroupRoleMappingPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdGroupRoleMapping> findByAdGroupIdentifier(String adGroupIdentifier) {
        return jpaRepository.findByAdGroupIdentifier(adGroupIdentifier).map(AdGroupRoleMappingPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdGroupRoleMapping> findAll() {
        return jpaRepository.findAll().stream().map(AdGroupRoleMappingPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public AdGroupRoleMapping save(AdGroupRoleMapping mapping) {
        AdGroupRoleMappingJpaEntity entity = jpaRepository.findById(mapping.id().toString())
                .orElseGet(() -> AdGroupRoleMappingPersistenceMapper.toNewJpa(mapping));
        entity.setAdGroupIdentifier(mapping.adGroupIdentifier());
        entity.setRoleId(mapping.roleId().toString());
        entity.setUpdatedBy(mapping.updatedBy() == null ? null : mapping.updatedBy().toString());
        entity.setUpdatedAt(mapping.updatedAt());
        try {
            // saveAndFlush: falla rápido aquí mismo ante la constraint UNIQUE(ad_group_identifier)
            // (INV-AUTH-010) en vez de diferir el error al flush implícito de otra unidad de
            // trabajo posterior — mismo criterio que JpaRefreshTokenRepositoryAdapter (Fase 9).
            jpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // RN-02 SPEC-AUTH-008: condición de carrera entre dos altas/modificaciones
            // concurrentes para el mismo grupo, pese a la verificación previa en la capa de
            // aplicación (traducción explícita exigida — no un 500 genérico).
            throw new AdGroupMappingAlreadyExistsException();
        }
        return mapping;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id.toString());
    }
}
