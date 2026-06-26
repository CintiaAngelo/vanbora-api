package com.vanbora.api.modules.guardian.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Dados do painel inicial do responsável. */
public record GuardianDashboardResponse(
        boolean hasTransporter,
        /** Dependente exibido neste painel. */
        Long dependentId,
        String studentName,
        /** Contrato pendente de assinatura deste dependente (em fase de contratação). */
        Long pendingContractId,
        /** Transportador contratado (para abrir a tela de avaliação). */
        Long transporterId,
        String transporterName,
        /** Pesquisa de satisfação vencida (nunca avaliou ou última > 3 meses). */
        boolean reviewDue,
        /** Vai hoje? (presença do dia atual). null = fim de semana / sem aula. */
        Boolean goingToday,
        List<DayAttendance> weekAttendance,
        NextPayment nextPayment,
        TransporterNotice notice,
        /** Solicitação pendente de aceite do transportador (acompanhamento na home). */
        Long pendingHireRequestId,
        String pendingHireTransporterName,
        Instant pendingHireExpiresAt,
        /** Solicitação recusada ainda não dispensada pelo responsável. */
        Long rejectedHireRequestId,
        String rejectedHireTransporterName
) {
    /** Presença em um dia da semana (rótulo SEG..SEX + data ISO + se vai). */
    public record DayAttendance(String day, String date, boolean present) {
    }

    /**
     * Próxima mensalidade. Pode ser uma cobrança real em aberto (payable=true)
     * ou uma projeção do próximo mês quando está tudo em dia (payable=false).
     */
    public record NextPayment(
            String referenceMonth,
            BigDecimal amount,
            LocalDate dueDate,
            String status,
            boolean payable) {
    }

    /** Aviso recente do transportador contratado. */
    public record TransporterNotice(String title, String message) {
    }
}
