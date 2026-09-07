package com.IntraNet.Laucom.security.application.password;

/** UC-AUTH-012 SPEC-AUTH-007. {@code token} es el valor en claro entregado por correo. */
public record ConfirmPasswordRecoveryCommand(String token, char[] newPassword) {
}
