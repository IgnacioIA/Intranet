package com.IntraNet.Laucom.security.application.admin;

import java.util.UUID;

/** UC-AUTH-013 SPEC-AUTH-008 (modificación). */
public record UpdateAdGroupMappingCommand(UUID mappingId, String adGroupIdentifier, UUID roleId) {
}
