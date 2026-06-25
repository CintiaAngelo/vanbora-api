package com.vanbora.api.modules.notification;

import com.vanbora.api.modules.hire.domain.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Notificações ao usuário. O e-mail é SIMULADO (logado) — sem SMTP. O mesmo
 * contrato também aparece no app (lista de pendentes), cobrindo "via e-mail/app".
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final String appBaseUrl;

    public NotificationService(@Value("${vanbora.app.base-url:vanbora://app}") String appBaseUrl) {
        this.appBaseUrl = appBaseUrl;
    }

    /** Avisa o responsável que há um contrato liberado para assinatura. */
    public void notifyContractReleased(Contract contract) {
        String to = contract.getGuardian().getUser().getEmail();
        String transporter = contract.getTransporter().getUser().getName();
        String link = "%s/contract/%d?t=%s".formatted(
                appBaseUrl, contract.getId(), contract.getSignatureToken());

        log.info("""
                [E-MAIL SIMULADO]
                  Para: {}
                  Assunto: Seu contrato com {} está pronto para assinatura
                  Corpo: Olá! O transportador {} liberou o contrato do(a) aluno(a) {}.
                         Acesse o app para revisar, cadastrar o pagamento e assinar.
                  Link de assinatura: {}""",
                to, transporter, transporter, contract.getDependent().getName(), link);
    }
}
