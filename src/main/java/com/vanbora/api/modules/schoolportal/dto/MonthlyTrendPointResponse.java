package com.vanbora.api.modules.schoolportal.dto;

/**
 * Novos vínculos (matrículas) num mês. {@code month} é 1-12 — o rótulo (nome do mês) é formatado
 * no frontend, respeitando o idioma selecionado no painel (pt-BR/en/es).
 */
public record MonthlyTrendPointResponse(int year, int month, long newLinks) {
}
