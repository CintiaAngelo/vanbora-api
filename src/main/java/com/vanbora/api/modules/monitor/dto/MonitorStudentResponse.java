package com.vanbora.api.modules.monitor.dto;

import java.util.List;

/**
 * Aluno de um percurso do monitor — só o necessário para o trabalho a bordo. Diferente do
 * {@code StudentResponse} do transportador, NÃO traz situação financeira.
 *
 * @param trips nomes dos percursos do monitor que levam este aluno
 */
public record MonitorStudentResponse(
        Long id,
        String name,
        String school,
        String guardianName,
        String neighborhood,
        boolean presentToday,
        List<String> trips
) {
}
