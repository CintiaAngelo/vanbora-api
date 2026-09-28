package com.vanbora.api.modules.trip.domain;

/**
 * Itens do checklist de segurança pós-percurso. Todos são obrigatórios: a conclusão
 * exige a confirmação de cada um + ao menos uma foto do interior da van.
 */
public enum ChecklistItem {
    INTERIOR_WALKTHROUGH("Percorri todo o interior da van, do primeiro ao último banco"),
    SEATS_AND_FLOOR("Conferi os bancos, o assoalho e embaixo dos assentos"),
    NO_CHILD_LEFT("Confirmo que nenhuma criança permaneceu no veículo");

    private final String label;

    ChecklistItem(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
