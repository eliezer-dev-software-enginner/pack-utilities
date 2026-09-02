package pack.utilities;

import java.util.regex.Pattern;

/**
 * Utilitário de validações comuns (e-mail, CEP, CPF, CNPJ, telefone e documento
 * combinado RG/CPF).
 *
 * <p>Padrão adotado por todos os métodos públicos desta classe:
 * <ul>
 *   <li>Nenhum método lança {@link NullPointerException}: o valor recebido é checado
 *       contra {@code null} logo na primeira linha;</li>
 *   <li>Convenção de nomes {@code isValidX(String value)}, retornando {@code true}
 *       quando o valor é considerado válido;</li>
 *   <li>A limpeza de máscara (pontos, traços, espaços, etc.) é feita internamente —
 *       o chamador pode passar o valor formatado ou não.</li>
 * </ul>
 */
public final class ValidatorPack {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$"
    );

    private ValidatorPack() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Valida o formato de um e-mail.
     *
     * @param email endereço a validar; {@code null} é considerado inválido.
     * @return {@code true} se {@code email} casar com o padrão esperado.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valida um CEP brasileiro, com ou sem máscara (ex.: "01310-100" ou "01310100").
     *
     * @param cep CEP a validar; {@code null} é considerado inválido.
     * @return {@code true} se, após remover caracteres não numéricos, restarem
     *         exatamente 8 dígitos.
     */
    public static boolean isValidCep(String cep) {
        if (cep == null) return false;
        String clean = cep.replaceAll("[^0-9]", "");
        return clean.length() == 8;
    }

    /**
     * Valida um CPF, com ou sem máscara, conferindo os dígitos verificadores
     * (algoritmo módulo 11) e rejeitando sequências de dígitos repetidos
     * (ex.: "111.111.111-11").
     *
     * @param cpf CPF a validar; {@code null} é considerado inválido.
     * @return {@code true} se o CPF for válido.
     */
    public static boolean isValidCpf(String cpf) {
        if (cpf == null) return false;
        String clean = cpf.replaceAll("[^0-9]", "");
        if (clean.length() != 11 || isAllSameChar(clean)) return false;

        int firstDigit = calculateCpfCheckDigit(clean.substring(0, 9));
        if (firstDigit != Character.getNumericValue(clean.charAt(9))) return false;

        int secondDigit = calculateCpfCheckDigit(clean.substring(0, 10));
        return secondDigit == Character.getNumericValue(clean.charAt(10));
    }

    /**
     * Valida um CNPJ, com ou sem máscara. Suporta tanto o formato numérico
     * tradicional (14 dígitos) quanto o formato alfanumérico, conferindo os
     * dígitos verificadores pelo algoritmo módulo 11.
     *
     * @param cnpj CNPJ a validar; {@code null} é considerado inválido.
     * @return {@code true} se o CNPJ for válido.
     */
    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null) return false;
        String clean = cnpj.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (clean.length() != 14) return false;
        // Os dois dígitos verificadores são sempre numéricos, mesmo no formato alfanumérico.
        if (!clean.substring(12).matches("\\d{2}") || isAllSameChar(clean)) return false;

        int firstDigit = calculateCnpjCheckDigit(clean.substring(0, 12));
        if (firstDigit != Character.getNumericValue(clean.charAt(12))) return false;

        int secondDigit = calculateCnpjCheckDigit(clean.substring(0, 13));
        return secondDigit == Character.getNumericValue(clean.charAt(13));
    }

    /**
     * Valida um valor que pode ser CPF (11 dígitos) ou CNPJ (14 caracteres,
     * numérico ou alfanumérico), delegando para {@link #isValidCpf} ou
     * {@link #isValidCnpj} conforme o tamanho encontrado.
     *
     * @param value CPF ou CNPJ a validar; {@code null} é considerado inválido.
     * @return {@code true} se o valor for um CPF ou CNPJ válido.
     */
    public static boolean isValidCpfOrCnpj(String value) {
        if (value == null) return false;
        String clean = value.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (clean.length() == 11) return isValidCpf(clean);
        if (clean.length() == 14) return isValidCnpj(clean);
        return false;
    }

    /**
     * Valida um documento de identificação que aceita RG (8-9 dígitos) ou CPF
     * (11 dígitos), no mesmo espírito do campo combinado {@code InputRgCpf}.
     *
     * <p>Diferente dos demais métodos desta classe, {@code null} ou vazio retornam
     * {@code true}: o documento é opcional e a validação só se aplica quando um
     * valor é informado.
     *
     * @param documento documento a validar; pode ser {@code null}.
     * @return {@code true} se vazio/nulo, ou se tiver 8-9 dígitos (RG) ou 11 dígitos (CPF).
     */
    public static boolean isValidDocumento(String documento) {
        if (documento == null) return true;
        String clean = documento.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return true;
        if (clean.length() <= 9) return clean.length() >= 8;
        return clean.length() == 11;
    }

    /**
     * Valida se o telefone tem 10 ou 11 dígitos numéricos (com ou sem DDD/máscara).
     *
     * @param phone telefone a validar; {@code null} é considerado inválido.
     * @return {@code true} se, após remover caracteres não numéricos, restarem
     *         10 ou 11 dígitos.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String clean = phone.replaceAll("[^0-9]", "");
        return clean.length() >= 10 && clean.length() <= 11;
    }

    /**
     * Calcula um dígito verificador de CPF pelo algoritmo módulo 11.
     *
     * @param base sequência de dígitos-base (9 dígitos para o 1º verificador,
     *             10 dígitos — incluindo o 1º verificador — para o 2º).
     * @return o dígito verificador calculado (0-9).
     */
    private static int calculateCpfCheckDigit(String base) {
        int weight = base.length() + 1;
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /**
     * Calcula um dígito verificador de CNPJ pelo algoritmo módulo 11, aceitando
     * caracteres alfanuméricos (cada caractere vale seu código ASCII menos 48,
     * conforme a especificação do CNPJ alfanumérico).
     *
     * @param base sequência-base (12 caracteres para o 1º verificador,
     *             13 — incluindo o 1º verificador — para o 2º).
     * @return o dígito verificador calculado (0-9).
     */
    private static int calculateCnpjCheckDigit(String base) {
        int[] weights = base.length() == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += (base.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /**
     * Verifica se todos os caracteres de uma string são iguais (usado para
     * rejeitar CPFs/CNPJs inválidos como "00000000000").
     */
    private static boolean isAllSameChar(String value) {
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) != value.charAt(0)) return false;
        }
        return true;
    }
}