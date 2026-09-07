package com.IntraNet.Laucom.security.infrastructure.persistence.adapter;

import com.IntraNet.Laucom.security.domain.model.AdGroupRoleMapping;
import com.IntraNet.Laucom.security.domain.port.AdGroupRoleMappingRepositoryPort;
import com.IntraNet.Laucom.security.infrastructure.persistence.mapper.AdGroupRoleMappingPersistenceMapper;
import com.IntraNet.Laucom.security.infrastructure.persistence.repository.AdGroupRoleMappingJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
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
}
