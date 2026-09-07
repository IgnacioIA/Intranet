package com.IntraNet.Laucom.security.application.admin;

/**
 * UC-AUTH-020 SPEC-AUTH-010 (modificación), RN-10. {@code requestedName} es lo que el cliente
 * envió en el cuerpo (si lo envió); {@code null} si no lo incluyó. Si viene y difiere de
 * {@code currentName}, es un intento de modificar el name inmutable.
 */
public record UpdatePermissionDescriptionCommand(String currentName, String requestedName, String description) {
}
