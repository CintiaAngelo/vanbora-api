package com.vanbora.api.modules.transporter.service;

import com.vanbora.api.modules.transporter.dto.DashboardResponse;

/** Caso de uso que compõe o painel inicial do transportador. */
public interface DashboardService {

    DashboardResponse getDashboard(Long transporterUserId);
}
