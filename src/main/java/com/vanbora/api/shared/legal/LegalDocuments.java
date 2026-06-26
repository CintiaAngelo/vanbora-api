package com.vanbora.api.shared.legal;

/** Versões vigentes dos documentos legais. Incremente ao alterar o conteúdo. */
public final class LegalDocuments {

    private LegalDocuments() {
    }

    /**
     * Versão atual da Política de Privacidade + Termos de Uso aceitos no cadastro.
     * Bump (ex.: "2026-07-01") sempre que o texto mudar, para que o consentimento
     * registrado seja rastreável à versão correta (LGPD, art. 8º §1º — demonstrável).
     */
    public static final String PRIVACY_TERMS_VERSION = "2026-06-26";
}
