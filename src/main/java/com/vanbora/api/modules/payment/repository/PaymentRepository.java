package com.vanbora.api.modules.payment.repository;

import com.vanbora.api.modules.payment.domain.Payment;
import com.vanbora.api.shared.enums.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByEnrollmentDependentGuardianIdOrderByReferenceMonthDesc(Long guardianId);

    /** Pagamentos de um dependente específico (visão por dependente). */
    List<Payment> findByEnrollmentDependentIdOrderByReferenceMonthDesc(Long dependentId);

    List<Payment> findByEnrollmentTransporterId(Long transporterId);

    List<Payment> findByEnrollmentTransporterIdAndStatus(Long transporterId, PaymentStatus status);

    /**
     * Pagamentos vencidos (dueDate efetivamente no passado, não pago), com aluno/responsável
     * pré-carregados — evita N+1 ao montar a lista de inadimplentes.
     */
    @Query("""
            select p from Payment p
            join fetch p.enrollment e
            join fetch e.dependent d
            join fetch d.guardian g
            join fetch g.user u
            where e.transporter.id = :transporterId
              and p.status <> com.vanbora.api.shared.enums.PaymentStatus.PAID
              and p.dueDate < :today
            order by p.dueDate asc
            """)
    List<Payment> findOverdueByTransporterId(@Param("transporterId") Long transporterId, @Param("today") LocalDate today);

    /**
     * Pagamentos não pagos ainda dentro do prazo (o contrário de vencido), mesmo pré-carregamento.
     * Exige dueDate preenchido — sem data não dá pra mostrar "vence em X dias" na lista.
     */
    @Query("""
            select p from Payment p
            join fetch p.enrollment e
            join fetch e.dependent d
            join fetch d.guardian g
            join fetch g.user u
            where e.transporter.id = :transporterId
              and p.status <> com.vanbora.api.shared.enums.PaymentStatus.PAID
              and p.dueDate is not null
              and p.dueDate >= :today
            order by p.dueDate asc
            """)
    List<Payment> findPendingByTransporterId(@Param("transporterId") Long transporterId, @Param("today") LocalDate today);
}
