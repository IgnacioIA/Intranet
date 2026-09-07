package com.IntraNet.Laucom.security.application.password;

import java.util.UUID;

/** UC-AUTH-010 SPEC-AUTH-007. {@code userId} lo resuelve el borde (Access Token) — Fase 4/8. */
public record ChangePasswordCommand(UUID userId, char[] currentPassword, char[] newPassword) {
}
