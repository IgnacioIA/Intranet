package com.IntraNet.Laucom.security.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void rejectsPasswordsShorterThanMinLength() {
        assertThat(PasswordPolicy.isValid("short".toCharArray())).isFalse();
    }

    @Test
    void acceptsPasswordsAtOrAboveMinLength() {
        assertThat(PasswordPolicy.isValid("a".repeat(PasswordPolicy.MIN_LENGTH).toCharArray())).isTrue();
    }

    @Test
    void rejectsNull() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }
}
