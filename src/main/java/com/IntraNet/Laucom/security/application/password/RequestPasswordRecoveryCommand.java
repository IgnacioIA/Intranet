package com.IntraNet.Laucom.security.application.password;

/** UC-AUTH-011 SPEC-AUTH-007. {@code clientIp} se usa como dimensión del rate limit (REQ-AUTH-018). */
public record RequestPasswordRecoveryCommand(String email, String clientIp) {
}
