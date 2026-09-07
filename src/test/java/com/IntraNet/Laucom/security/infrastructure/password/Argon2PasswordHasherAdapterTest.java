package com.IntraNet.Laucom.security.infrastructure.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** ADR-011. No requiere contexto de Spring: los parámetros de {@code @Value} son solo argumentos de constructor. */
class Argon2PasswordHasherAdapterTest {

    private final Argon2PasswordHasherAdapter adapter = new Argon2PasswordHasherAdapter(16, 32, 1, 16384, 2);

    @Test
    void hash_neverEqualsPlainText_andMatchesVerifiesCorrectly() {
        char[] password = "correct horse battery staple".toCharArray();

        String hash = adapter.hash(password);

        assertThat(hash).isNotEqualTo(new String(password));
        assertThat(adapter.matches(password, hash)).isTrue();
        assertThat(adapter.matches("wrong password entirely".toCharArray(), hash)).isFalse();
    }

    @Test
    void hash_isSalted_samePassword_producesDifferentHashesEachTime() {
        char[] password = "correct horse battery staple".toCharArray();

        String hash1 = adapter.hash(password);
        String hash2 = adapter.hash(password);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(adapter.matches(password, hash1)).isTrue();
        assertThat(adapter.matches(password, hash2)).isTrue();
    }
}
