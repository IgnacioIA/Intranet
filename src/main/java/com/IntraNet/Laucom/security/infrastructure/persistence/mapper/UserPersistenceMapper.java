package com.IntraNet.Laucom.security.infrastructure.persistence.mapper;

import com.IntraNet.Laucom.security.domain.model.IdentityProvider;
import com.IntraNet.Laucom.security.domain.model.PasswordCredential;
import com.IntraNet.Laucom.security.domain.model.RoleProvenance;
import com.IntraNet.Laucom.security.domain.model.User;
import com.IntraNet.Laucom.security.domain.model.UserRoleAssignment;
import com.IntraNet.Laucom.security.domain.model.UserStatus;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserJpaEntity;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserRoleAssignmentId;
import com.IntraNet.Laucom.security.infrastructure.persistence.entity.UserRoleAssignmentJpaEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static UserJpaEntity toNewJpa(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.id().toString());
        entity.setProvider(user.provider().name());
        entity.setExternalId(user.externalId().orElse(null));
        entity.setUsername(user.username());
        entity.setDisplayName(user.displayName());
        entity.setEmail(user.email().orElse(null));
        entity.setStatus(user.status().name());
        user.credential().ifPresentOrElse(
                c -> {
                    entity.setPasswordHash(c.hash());
                    entity.setMustChangePasswordOnNextLogin(c.mustChangeOnNextLogin());
                },
                () -> {
                    entity.setPasswordHash(null);
                    entity.setMustChangePasswordOnNextLogin(false);
                });
        entity.setCreatedAt(user.createdAt());
        entity.setLastLoginAt(user.lastLoginAt().orElse(null));
        entity.setLastAdSyncAt(user.lastAdSyncAt().orElse(null));
        entity.setFailedLoginAttempts(user.failedLoginAttempts());
        entity.setLockedUntil(user.lockedUntil().orElse(null));
        return entity;
    }

    public static List<UserRoleAssignmentJpaEntity> toAssignmentJpa(User user) {
        return user.roles().stream()
                .map(a -> new UserRoleAssignmentJpaEntity(
                        new UserRoleAssignmentId(user.id().toString(), a.roleId().toString()),
                        a.provenance().name(),
                        a.sourceAdGroup(),
                        a.assignedAt()))
                .collect(Collectors.toList());
    }

    public static User toDomain(UserJpaEntity entity, List<UserRoleAssignmentJpaEntity> assignments) {
        PasswordCredential credential = entity.getPasswordHash() == null
                ? null
                : PasswordCredential.of(entity.getPasswordHash(), entity.isMustChangePasswordOnNextLogin());
        Set<UserRoleAssignment> roleAssignments = assignments.stream()
                .map(a -> new UserRoleAssignment(
                        UUID.fromString(a.getId().getRoleId()),
                        RoleProvenance.valueOf(a.getProvenance()),
                        a.getSourceAdGroup(),
                        a.getAssignedAt()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return User.reconstitute(
                UUID.fromString(entity.getId()),
                IdentityProvider.valueOf(entity.getProvider()),
                entity.getExternalId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getEmail(),
                UserStatus.valueOf(entity.getStatus()),
                credential,
                roleAssignments,
                entity.getCreatedAt(),
                entity.getLastLoginAt(),
                entity.getLastAdSyncAt(),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil());
    }
}
