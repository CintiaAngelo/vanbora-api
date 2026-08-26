package com.vanbora.api.shared.validation;

import java.util.Locale;

/**
 * Padronização de nomes livres (escola, bairro): primeira letra de cada palavra em
 * maiúscula, restante em minúscula. Preserva acentos — não remove diacríticos, apenas
 * usa {@code Locale} pt-BR para maiúsculas/minúsculas corretas em "ã", "é", "ç" etc.
 */
public final class TextNormalization {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private TextNormalization() {
    }

    /** "VILA MARIANA" / "vila mariana" / "ViLa MaRiAnA" → "Vila Mariana". Colapsa espaços extras. */
    public static String titleCase(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String[] words = trimmed.split(" ");
        StringBuilder result = new StringBuilder(trimmed.length());
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i];
            result.append(word.substring(0, 1).toUpperCase(PT_BR));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase(PT_BR));
            }
        }
        return result.toString();
    }
}
