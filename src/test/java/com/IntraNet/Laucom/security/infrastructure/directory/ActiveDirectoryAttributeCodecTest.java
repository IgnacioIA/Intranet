package com.IntraNet.Laucom.security.infrastructure.directory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Funciones puras de ActiveDirectoryAdapter, verificables sin un directorio real.
 *
 * <p>Los caracteres especiales del filtro LDAP (RFC 4515) se construyen aquí a partir de sus
 * códigos numéricos, en vez de como literales de caracter, para evitar cualquier ambigüedad de
 * codificación al transcribir el archivo.</p>
 */
class ActiveDirectoryAttributeCodecTest {

    private static final char ASTERISK = (char) 0x2A;
    private static final char PAREN_OPEN = (char) 0x28;
    private static final char PAREN_CLOSE = (char) 0x29;
    private static final char BACKSLASH = (char) 0x5C;
    private static final char NUL = (char) 0x00;

    @Test
    void toStableString_convertsKnownByteVector_toCanonicalGuidString() {
        // Vector de prueba conocido: bytes crudos tal como los devolvería AD vía LDAP para el
        // GUID canónico "12345678-1234-5678-1234-567812345678" (ejemplo de referencia RFC/MS-DTYP).
        byte[] raw = {
                (byte) 0x78, (byte) 0x56, (byte) 0x34, (byte) 0x12, // Data1, little-endian
                (byte) 0x34, (byte) 0x12,                           // Data2, little-endian
                (byte) 0x78, (byte) 0x56,                           // Data3, little-endian
                (byte) 0x12, (byte) 0x34,                           // Data4[0..1], tal cual
                (byte) 0x56, (byte) 0x78, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 // Data4[2..7]
        };

        String result = ActiveDirectoryAttributeCodec.toStableString(raw);

        assertThat(result).isEqualTo("12345678-1234-5678-1234-567812345678");
    }

    @Test
    void toStableString_rejectsWrongLength() {
        assertThatThrownBy(() -> ActiveDirectoryAttributeCodec.toStableString(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStableString_rejectsNull() {
        assertThatThrownBy(() -> ActiveDirectoryAttributeCodec.toStableString(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractGroupIdentifier_readsCnComponent() {
        String dn = "CN=IT-SUPPORT,OU=Groups,DC=example,DC=com";

        assertThat(ActiveDirectoryAttributeCodec.extractGroupIdentifier(dn)).isEqualTo("IT-SUPPORT");
    }

    @Test
    void extractGroupIdentifier_isCaseInsensitiveOnCnLabel() {
        String dn = "cn=Finance-Team,OU=Groups,DC=example,DC=com";

        assertThat(ActiveDirectoryAttributeCodec.extractGroupIdentifier(dn)).isEqualTo("Finance-Team");
    }

    @Test
    void extractGroupIdentifier_rejectsDnWithoutCn() {
        assertThatThrownBy(() -> ActiveDirectoryAttributeCodec.extractGroupIdentifier("OU=Groups,DC=example,DC=com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractGroupIdentifier_rejectsBlank() {
        assertThatThrownBy(() -> ActiveDirectoryAttributeCodec.extractGroupIdentifier("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void escapeForFilter_escapesAsterisk() {
        String input = "a" + ASTERISK + "b";

        assertThat(ActiveDirectoryAttributeCodec.escapeForFilter(input)).isEqualTo("a\\2ab");
    }

    @Test
    void escapeForFilter_escapesParentheses() {
        String input = "a" + PAREN_OPEN + "b" + PAREN_CLOSE + "c";

        assertThat(ActiveDirectoryAttributeCodec.escapeForFilter(input)).isEqualTo("a\\28b\\29c");
    }

    @Test
    void escapeForFilter_escapesBackslash() {
        String input = "a" + BACKSLASH + "b";

        assertThat(ActiveDirectoryAttributeCodec.escapeForFilter(input)).isEqualTo("a\\5cb");
    }

    @Test
    void escapeForFilter_escapesNulByte() {
        String input = "a" + NUL + "b";

        assertThat(ActiveDirectoryAttributeCodec.escapeForFilter(input)).isEqualTo("a\\00b");
    }

    @Test
    void escapeForFilter_leavesOrdinaryTextUnchanged() {
        assertThat(ActiveDirectoryAttributeCodec.escapeForFilter("jdoe")).isEqualTo("jdoe");
        assertThat(ActiveDirectoryAttributeCodec.escapeForFilter("Jane-Doe")).isEqualTo("Jane-Doe");
    }
}
