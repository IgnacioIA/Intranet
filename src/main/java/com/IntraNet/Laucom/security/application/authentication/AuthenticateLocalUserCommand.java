package com.IntraNet.Laucom.security.application.authentication;

/** UC-AUTH-001 SPEC-AUTH-001. */
public record AuthenticateLocalUserCommand(String username, char[] password, String clientIp) {
}
