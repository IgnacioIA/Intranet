package com.IntraNet.Laucom.security.infrastructure.persistence.repository;

import com.IntraNet.Laucom.security.infrastructure.persistence.entity.AdGroupRoleMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface AdGroupRoleMappingJpaRepository extends JpaRepository<AdGroupRoleMappingJpaEntity, String> {

    Set<AdGroupRoleMappingJpaEntity> findByAdGroupIdentifierIn(Collection<String> adGroupIdentifiers);

    /** UC-AUTH-013 SPEC-AUTH-008 RN-02: verificación de unicidad en alta/modificación. */
    Optional<AdGroupRoleMappingJpaEntity> findByAdGroupIdentifier(String adGroupIdentifier);
}
