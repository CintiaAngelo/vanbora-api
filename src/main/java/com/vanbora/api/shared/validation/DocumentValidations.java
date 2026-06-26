package com.vanbora.api.shared.validation;

/**
 * Validações de campos de cadastro (CPF, CNH, telefone, e-mail, placa).
 * Espelha as regras aplicadas no app para defesa em profundidade no backend.
 */
public final class DocumentValidations {

    private DocumentValidations() {
    }

    /** Remove tudo que não for dígito. */
    public static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    /** Telefone válido = 11 dígitos (DDD + 9 do celular). */
    public static boolean isValidPhone(String value) {
        return onlyDigits(value).length() == 11;
    }

    /** E-mail válido = "@" + domínio + terminação (.com, .com.br, .net, etc.). */
    public static boolean isValidEmail(String value) {
        return value != null && value.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    }

    /** Placa válida = 1 a 7 caracteres alfanuméricos. */
    public static boolean isValidPlate(String value) {
        if (value == null) {
            return false;
        }
        String p = value.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return p.length() >= 1 && p.length() <= 7;
    }

    /** Valida CPF (11 dígitos + dígitos verificadores). */
    public static boolean isValidCpf(String value) {
        String c = onlyDigits(value);
        if (c.length() != 11 || c.chars().distinct().count() == 1) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (c.charAt(i) - '0') * (10 - i);
        }
        int d1 = (sum * 10) % 11;
        if (d1 == 10) {
            d1 = 0;
        }
        if (d1 != c.charAt(9) - '0') {
            return false;
        }
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (c.charAt(i) - '0') * (11 - i);
        }
        int d2 = (sum * 10) % 11;
        if (d2 == 10) {
            d2 = 0;
        }
        return d2 == c.charAt(10) - '0';
    }

    /** Valida CNH no padrão oficial: 9 dígitos base + 2 verificadores (11 no total). */
    public static boolean isValidCnh(String value) {
        String c = onlyDigits(value);
        if (c.length() != 11 || c.chars().distinct().count() == 1) {
            return false;
        }
        int dsc = 0;
        int sum = 0;
        for (int i = 0, j = 9; i < 9; i++, j--) {
            sum += (c.charAt(i) - '0') * j;
        }
        int d1 = sum % 11;
        if (d1 >= 10) {
            d1 = 0;
            dsc = 2;
        }
        sum = 0;
        for (int i = 0, j = 1; i < 9; i++, j++) {
            sum += (c.charAt(i) - '0') * j;
        }
        int r = sum % 11;
        int d2 = r >= 10 ? 0 : r - dsc;
        if (d2 < 0) {
            d2 += 11;
        }
        return d1 == c.charAt(9) - '0' && d2 == c.charAt(10) - '0';
    }
}
