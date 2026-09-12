package com.IntraNet.Laucom.security.application.admin;

import java.util.UUID;

/** UC-AUTH-013 SPEC-AUTH-008 (alta). */
public record CreateAdGroupMappingCommand(String adGroupIdentifier, UUID roleId) {
}
