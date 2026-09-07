package com.IntraNet.Laucom.security.application.authentication;

/** UC-AUTH-002 SPEC-AUTH-001. */
public record AuthenticateActiveDirectoryUserCommand(String username, char[] password, String clientIp) {
}
