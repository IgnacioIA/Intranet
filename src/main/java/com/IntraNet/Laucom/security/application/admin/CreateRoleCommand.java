package com.IntraNet.Laucom.security.application.admin;

import java.util.Set;

/** UC-AUTH-019 SPEC-AUTH-010 (alta). */
public record CreateRoleCommand(String name, String description, Set<String> permissionNames) {
}
