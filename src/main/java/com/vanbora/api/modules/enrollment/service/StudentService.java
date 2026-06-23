package com.vanbora.api.modules.enrollment.service;

import com.vanbora.api.modules.enrollment.dto.StudentResponse;
import com.vanbora.api.shared.enums.FinanceStatus;
import java.util.List;

/** Casos de uso da lista de alunos (matrículas) de um transportador. */
public interface StudentService {

    /**
     * Lista os alunos do transportador autenticado, opcionalmente filtrando
     * apenas inadimplentes.
     */
    List<StudentResponse> listMyStudents(Long transporterUserId, FinanceStatus financeStatus);
}
