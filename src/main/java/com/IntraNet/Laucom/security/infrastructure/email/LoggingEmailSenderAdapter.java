package com.IntraNet.Laucom.security.infrastructure.email;

import com.IntraNet.Laucom.security.domain.port.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder de {@link EmailSenderPort}: solo registra (Application Log técnico, no Security
 * Audit) que un correo de recuperación debería enviarse — <b>nunca</b> el token en claro
 * (REQ-AUTH-023). No envía correo real.
 *
 * <p>La aplicación anfitriona debe reemplazar este bean por un adapter real (ej. basado en
 * {@code JavaMailSender}) antes de un despliegue de producción. Documentado como limitación
 * conocida del checkpoint de Fase 3 — el envío de correo en sí excede el alcance del módulo de
 * seguridad.</p>
 */
@Component
public class LoggingEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSenderAdapter.class);

    @Override
    public void sendPasswordRecoveryEmail(String toEmail, String recoveryTokenPlainValue) {
        log.info("[placeholder] Se solicitó el envío de un correo de recuperación de contraseña. "
                + "Ningún correo real fue enviado — reemplazar EmailSenderPort por un adapter real "
                + "antes de producción.");
    }
}
