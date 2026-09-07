package com.IntraNet.Laucom.security.infrastructure.token;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** REQ-AUTH-026 / ADR-009: sin claves configuradas, la aplicación debe fallar rápido, nunca
 * arrancar con una clave insegura implícita. */
class JwtSigningKeysTest {

    private static String validPrivateKeyBase64() throws Exception {
        return Base64.getEncoder().encodeToString(generateKeyPair().getPrivate().getEncoded());
    }

    private static String validPublicKeyBase64() throws Exception {
        return Base64.getEncoder().encodeToString(generateKeyPair().getPublic().getEncoded());
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void loadsAValidKeyPair() throws Exception {
        KeyPair pair = generateKeyPair();
        String privateB64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String publicB64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        JwtSigningKeys keys = new JwtSigningKeys(privateB64, publicB64, "kid-1");

        assertThat(keys.privateKey()).isNotNull();
        assertThat(keys.publicKey()).isNotNull();
        assertThat(keys.keyId()).isEqualTo("kid-1");
    }

    @Test
    void rejectsMissingPrivateKey() throws Exception {
        assertThatThrownBy(() -> new JwtSigningKeys("", validPublicKeyBase64(), "kid-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.private-key-base64");
    }

    @Test
    void rejectsMissingPublicKey() throws Exception {
        assertThatThrownBy(() -> new JwtSigningKeys(validPrivateKeyBase64(), "", "kid-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.public-key-base64");
    }

    @Test
    void rejectsMissingKid() throws Exception {
        assertThatThrownBy(() -> new JwtSigningKeys(validPrivateKeyBase64(), validPublicKeyBase64(), ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.kid");
    }

    @Test
    void rejectsMalformedBase64Content() throws Exception {
        assertThatThrownBy(() -> new JwtSigningKeys("not-valid-base64-!!!", validPublicKeyBase64(), "kid-1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
