package com.IntraNet.Laucom.security.domain.port;

/**
 * Envío de correo — infraestructura, no comportamiento de dominio (SPEC-AUTH-007, UC-AUTH-011).
 * La aplicación anfitriona debe proveer un adapter real (ej. basado en {@code JavaMailSender})
 * antes de producción; el adapter incluido en este módulo es un placeholder que solo registra
 * la intención de envío (ver {@code LoggingEmailSenderAdapter}).
 */
public interface EmailSenderPort {

    void sendPasswordRecoveryEmail(String toEmail, String recoveryTokenPlainValue);
}
