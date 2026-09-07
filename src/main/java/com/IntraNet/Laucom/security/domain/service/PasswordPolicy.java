package com.IntraNet.Laucom.security.domain.service;

/**
 * Política de contraseñas para usuarios {@code LOCAL}. SPEC-AUTH-007 §6 delega el parámetro
 * concreto a la implementación ("longitud mínima, etc. — parámetros a definir en
 * implementación"): esta clase materializa esa decisión ya autorizada por la propia SPEC
 * (decisión de Nivel 1/2 según .claude/core/decision-authority.md — de bajo impacto y
 * trivialmente reversible, no requiere un ADR nuevo).
 *
 * <p>Se exige longitud mínima sin reglas arbitrarias de complejidad (mayúscula/símbolo/etc.),
 * siguiendo la orientación moderna de NIST SP 800-63B: la longitud es un mejor predictor de
 * resistencia que la complejidad forzada, que tiende a producir patrones predecibles.</p>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;

    private PasswordPolicy() {
    }

    public static boolean isValid(char[] password) {
        return password != null && password.length >= MIN_LENGTH;
    }
}
