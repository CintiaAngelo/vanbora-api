package com.vanbora.api.modules.hire.service;

import com.vanbora.api.modules.hire.domain.Contract;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Monta o texto do contrato exibido ao responsável na assinatura. Sempre gera um
 * cabeçalho padrão com as partes e os dados essenciais; o corpo é o modelo do
 * transportador (quando houver) ou um texto padrão de prestação de serviço.
 */
@Component
public class ContractTextBuilder {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Constrói o texto completo (cabeçalho + corpo) a partir do contrato liberado. */
    public String build(Contract contract) {
        String guardian = contract.getGuardian().getUser().getName();
        String transporter = contract.getTransporter().getUser().getName();
        String student = contract.getDependent().getName();
        String school = contract.getDependent().getSchool();
        String fee = formatCurrency(contract.getMonthlyFee());

        String header = """
                CONTRATO DE PRESTAÇÃO DE SERVIÇO DE TRANSPORTE ESCOLAR

                CONTRATANTE (Responsável): %s
                CONTRATADO (Transportador): %s
                ALUNO(A) transportado(a): %s
                Escola: %s
                Mensalidade: %s
                Data: %s
                """.formatted(
                guardian,
                transporter,
                student,
                school == null || school.isBlank() ? "(a definir)" : school,
                fee,
                LocalDate.now().format(DATE));

        String template = contract.getTransporter().getContractTemplate();
        String body = (template != null && !template.isBlank())
                ? template.trim()
                : defaultBody(fee);

        return header + "\n" + body;
    }

    private String defaultBody(String fee) {
        return """
                CLÁUSULA 1ª — DO OBJETO
                O CONTRATADO prestará ao CONTRATANTE o serviço de transporte escolar do(a) aluno(a) acima
                identificado(a), no trajeto entre o endereço de embarque informado e a escola, e o retorno,
                nos dias letivos, conforme a rota e os horários combinados entre as partes.

                CLÁUSULA 2ª — DO VALOR E DO PAGAMENTO
                Pelo serviço, o CONTRATANTE pagará a mensalidade de %s, com vencimento mensal, processada
                pela plataforma VanBora por meio de provedor de pagamentos. A primeira mensalidade é cobrada
                no ato da assinatura deste contrato.

                CLÁUSULA 3ª — DA VIGÊNCIA
                Este contrato vigora por prazo indeterminado a partir da assinatura, podendo ser encerrado
                por qualquer das partes, na forma da Cláusula 5ª.

                CLÁUSULA 4ª — DAS OBRIGAÇÕES
                4.1. O CONTRATADO compromete-se a realizar o transporte com segurança, pontualidade e veículo
                em condições regulares, mantendo documentação e habilitação válidas.
                4.2. O CONTRATANTE compromete-se a manter os dados atualizados, efetuar os pagamentos em dia e
                deixar o(a) aluno(a) pronto(a) no horário e local combinados.

                CLÁUSULA 5ª — DO CANCELAMENTO
                Qualquer das partes poderá encerrar o contrato mediante comunicação pela plataforma. O
                cancelamento encerra a matrícula do(a) aluno(a) deste contrato a partir da solicitação.

                CLÁUSULA 6ª — DA PROTEÇÃO DE DADOS (LGPD)
                Os dados pessoais tratados destinam-se exclusivamente à execução deste contrato e à operação
                do serviço, observada a Lei nº 13.709/2018 (LGPD) e a Política de Privacidade da plataforma.

                CLÁUSULA 7ª — DO FORO
                Fica eleito o foro do domicílio do CONTRATANTE para dirimir eventuais dúvidas oriundas deste
                contrato.

                Ao assinar, o CONTRATANTE declara ter lido e concordado com os termos acima.
                """.formatted(fee);
    }

    private String formatCurrency(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return NumberFormat.getCurrencyInstance(PT_BR).format(amount);
    }
}
