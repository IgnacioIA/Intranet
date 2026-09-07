package com.IntraNet.Laucom.security.application.admin;

/** UC-AUTH-016 SPEC-AUTH-010. {@code initialPassword} puede ser {@code null}/vacío: el sistema genera una. */
public record CreateLocalUserCommand(String username, String email, String displayName, char[] initialPassword) {
}
