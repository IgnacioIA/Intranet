package com.IntraNet.Laucom.security.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpaqueTokenGeneratorTest {

    @Test
    void generateSecret_producesHighEntropyUniqueValues() {
        String a = OpaqueTokenGenerator.generateSecret();
        String b = OpaqueTokenGenerator.generateSecret();

        assertThat(a).isNotEqualTo(b);
        assertThat(a.length()).isGreaterThanOrEqualTo(32);
    }

    @Test
    void hash_isDeterministic_forSameInput() {
        String secret = OpaqueTokenGenerator.generateSecret();

        assertThat(OpaqueTokenGenerator.hash(secret)).isEqualTo(OpaqueTokenGenerator.hash(secret));
    }

    @Test
    void hash_differsForDifferentSecrets() {
        assertThat(OpaqueTokenGenerator.hash("a")).isNotEqualTo(OpaqueTokenGenerator.hash("b"));
    }

    @Test
    void hash_neverEqualsTheOriginalSecret() {
        String secret = OpaqueTokenGenerator.generateSecret();

        assertThat(OpaqueTokenGenerator.hash(secret)).isNotEqualTo(secret);
    }
}
