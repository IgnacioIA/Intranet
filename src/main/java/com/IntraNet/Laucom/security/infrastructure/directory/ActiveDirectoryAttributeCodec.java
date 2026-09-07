package com.IntraNet.Laucom.security.infrastructure.directory;

/**
 * Funciones puras de traducción de atributos LDAP/AD, extraídas de {@link ActiveDirectoryAdapter}
 * para poder testearlas sin un directorio real. Ver ADR-004 (objectGUID como identificador
 * estable).
 */
final class ActiveDirectoryAttributeCodec {

    private ActiveDirectoryAttributeCodec() {
    }

    /**
     * Convierte el valor binario crudo de {@code objectGUID} (16 bytes, tal como lo devuelve
     * AD vía LDAP) a su representación canónica de texto (la misma que muestran las
     * herramientas administrativas de Microsoft, ej. {@code Get-ADUser -Properties objectGUID}).
     *
     * <p>AD almacena los primeros tres componentes del GUID en little-endian; el cuarto
     * (los últimos 8 bytes) se conserva tal cual. Por eso no alcanza con un volcado hexadecimal
     * directo de los 16 bytes.</p>
     */
    static String toStableString(byte[] objectGuid) {
        if (objectGuid == null || objectGuid.length != 16) {
            throw new IllegalArgumentException("objectGUID debe tener exactamente 16 bytes");
        }
        StringBuilder sb = new StringBuilder(36);
        appendReversedHex(sb, objectGuid, 0, 4);
        sb.append('-');
        appendReversedHex(sb, objectGuid, 4, 2);
        sb.append('-');
        appendReversedHex(sb, objectGuid, 6, 2);
        sb.append('-');
        appendHex(sb, objectGuid, 8, 2);
        sb.append('-');
        appendHex(sb, objectGuid, 10, 6);
        return sb.toString();
    }

    private static void appendReversedHex(StringBuilder sb, byte[] bytes, int offset, int length) {
        for (int i = offset + length - 1; i >= offset; i--) {
            appendByteHex(sb, bytes[i]);
        }
    }

    private static void appendHex(StringBuilder sb, byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            appendByteHex(sb, bytes[i]);
        }
    }

    private static void appendByteHex(StringBuilder sb, byte b) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
    }

    /**
     * Extrae el identificador de grupo (valor del componente {@code CN}) de un Distinguished
     * Name devuelto en {@code memberOf}, ej. {@code "CN=IT-SUPPORT,OU=Groups,DC=example,DC=com"}
     * → {@code "IT-SUPPORT"}. Es ese valor — no la DN completa — el que se compara contra
     * {@code AdGroupRoleMapping.adGroupIdentifier} (ver SPEC-AUTH-008, ejemplos).
     */
    static String extractGroupIdentifier(String distinguishedName) {
        if (distinguishedName == null || distinguishedName.isBlank()) {
            throw new IllegalArgumentException("distinguishedName no puede estar vacío");
        }
        for (String component : distinguishedName.split(",")) {
            String trimmed = component.trim();
            if (trimmed.regionMatches(true, 0, "CN=", 0, 3)) {
                return trimmed.substring(3);
            }
        }
        throw new IllegalArgumentException("DN sin componente CN: " + distinguishedName);
    }

    /**
     * Escapa un valor de entrada de usuario para uso seguro dentro de un filtro de búsqueda
     * LDAP (RFC 4515) — previene inyección de filtro LDAP a partir de un {@code username}
     * provisto por el usuario (CLAUDE.md, sección Seguridad: "Validación de entradas").
     */
    static String escapeForFilter(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
