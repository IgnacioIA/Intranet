package com.IntraNet.Laucom.security.application.session;

/** UC-AUTH-007 SPEC-AUTH-003. Mismo criterio de identidad que {@link LogoutCommand}. */
public record LogoutAllCommand(String refreshTokenSecret) {
}
