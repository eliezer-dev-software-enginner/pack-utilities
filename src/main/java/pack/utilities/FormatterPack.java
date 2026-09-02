package pack.utilities;

/**
 * Utilitário de formatação (aplicação de máscara) para telefone, CEP, CPF, CNPJ e para os
 * campos combinados CPF/CNPJ e RG/CPF.
 *
 * <p>Padrão adotado por todos os métodos públicos desta classe:
 * <ul>
 *   <li>Nenhum método lança {@link NullPointerException}: o valor recebido é checado
 *       contra {@code null} logo na primeira linha, retornando {@code ""} nesse caso;</li>
 *   <li>O valor é sanitizado (máscara removida) internamente — o chamador pode passar o
 *       valor já formatado ou não, assim como no {@link ValidatorPack};</li>
 *   <li>A formatação é progressiva: funciona também com valores parciais (ex.: durante a
 *       digitação em um campo de formulário), aplicando a máscara apenas até onde os
 *       caracteres disponíveis permitem.</li>
 * </ul>
 */
public final class FormatterPack {

    private FormatterPack() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Aplica a máscara de telefone brasileiro, dinamicamente:
     * {@code (XX) XXXX-XXXX} para fixo (8 dígitos locais) ou {@code (XX) XXXXX-XXXX}
     * para celular (9 dígitos locais).
     *
     * <p>Enquanto o número local tiver 5 dígitos ou menos, nenhum traço é inserido —
     * não é possível saber ainda se o número final terá 8 ou 9 dígitos locais, logo
     * não dá pra saber a posição correta do traço.
     *
     * @param raw telefone a formatar, com ou sem máscara; {@code null} retorna {@code ""}.
     * @return telefone formatado, ou {@code ""} se {@code raw} for nulo/vazio/sem dígitos.
     */
    public static String formatPhone(String raw) {
        if (raw == null) return "";
        String numeric = raw.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        sb.append("(");
        if (len <= 2) {
            sb.append(numeric);
        } else {
            sb.append(numeric, 0, 2).append(") ");
            String rest = numeric.substring(2);

            if (rest.length() <= 5) {
                // Ainda não dá pra saber se vai virar fixo (8) ou celular (9 dígitos).
                sb.append(rest);
            } else if (rest.length() <= 8) {
                // Formato Fixo: (XX) XXXX-XXXX
                sb.append(rest, 0, 4).append("-").append(rest.substring(4));
            } else {
                // Formato Celular: (XX) XXXXX-XXXX
                sb.append(rest, 0, 5).append("-").append(rest.substring(5));
            }
        }
        return sb.toString();
    }

    /**
     * Aplica a máscara de CEP: {@code XXXXX-XXX}.
     *
     * @param raw CEP a formatar, com ou sem máscara; {@code null} retorna {@code ""}.
     * @return CEP formatado, ou {@code ""} se {@code raw} for nulo/vazio/sem dígitos.
     */
    public static String formatCep(String raw) {
        if (raw == null) return "";
        String numeric = raw.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 5) {
            sb.append(numeric);
        } else {
            sb.append(numeric, 0, 5).append("-").append(numeric.substring(5));
        }

        return sb.toString();
    }

    /**
     * Aplica a máscara de CPF: {@code XXX.XXX.XXX-XX}.
     *
     * @param raw CPF a formatar, com ou sem máscara; {@code null} retorna {@code ""}.
     * @return CPF formatado, ou {@code ""} se {@code raw} for nulo/vazio/sem dígitos.
     */
    public static String formatCpf(String raw) {
        if (raw == null) return "";
        String numeric = raw.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 3) {
            sb.append(numeric);
        } else if (len <= 6) {
            sb.append(numeric, 0, 3).append(".").append(numeric.substring(3));
        } else if (len <= 9) {
            sb.append(numeric, 0, 3).append(".").append(numeric, 3, 6).append(".").append(numeric.substring(6));
        } else {
            sb.append(numeric, 0, 3).append(".")
                    .append(numeric, 3, 6).append(".")
                    .append(numeric, 6, 9).append("-")
                    .append(numeric.substring(9));
        }

        return sb.toString();
    }

    /**
     * Aplica a máscara de CNPJ: {@code XX.XXX.XXX/XXXX-XX}. Suporta tanto o CNPJ numérico
     * tradicional quanto o formato alfanumérico mais recente (letras nos 12 primeiros
     * caracteres); os 2 dígitos verificadores finais são sempre numéricos.
     *
     * @param raw CNPJ a formatar, com ou sem máscara; {@code null} retorna {@code ""}.
     * @return CNPJ formatado, ou {@code ""} se {@code raw} for nulo/vazio/sem caracteres válidos.
     */
    public static String formatCnpj(String raw) {
        if (raw == null) return "";
        String clean = raw.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (clean.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = clean.length();

        if (len <= 2) {
            sb.append(clean);
        } else if (len <= 5) {
            sb.append(clean, 0, 2).append(".").append(clean.substring(2));
        } else if (len <= 8) {
            sb.append(clean, 0, 2).append(".").append(clean, 2, 5).append(".").append(clean.substring(5));
        } else if (len <= 12) {
            sb.append(clean, 0, 2).append(".").append(clean, 2, 5).append(".")
                    .append(clean, 5, 8).append("/").append(clean.substring(8));
        } else {
            sb.append(clean, 0, 2).append(".").append(clean, 2, 5).append(".")
                    .append(clean, 5, 8).append("/").append(clean, 8, 12).append("-")
                    .append(clean.substring(12));
        }

        return sb.toString();
    }

    /**
     * Formata um campo combinado CPF-ou-CNPJ: enquanto o valor tiver 11 caracteres
     * alfanuméricos ou menos, assume CPF (puramente numérico); a partir do 12º, assume
     * CNPJ (aceita letras, formato alfanumérico mais recente). O reagrupamento dos
     * separadores ao cruzar esse limiar é esperado — sem perguntar de antemão qual
     * documento é, não tem como saber os grupos certos antes de ver o tamanho final.
     *
     * @param raw valor a formatar, com ou sem máscara; {@code null} retorna {@code ""}.
     * @return CPF ou CNPJ formatado, ou {@code ""} se {@code raw} for nulo/vazio/sem
     *         caracteres válidos.
     */
    public static String formatCpfCnpj(String raw) {
        if (raw == null) return "";
        String alnum = raw.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (alnum.isEmpty()) return "";
        if (alnum.length() <= 11) return formatCpf(alnum);
        return formatCnpj(alnum);
    }

    /**
     * Formata um campo combinado RG-ou-CPF: até 9 dígitos assume RG (máscara
     * {@code ##.###.###-#}), a partir do 10º assume CPF ({@code ###.###.###-##}). O
     * reagrupamento dos separadores ao cruzar esse limiar é esperado — sem perguntar de
     * antemão qual documento é, não tem como saber os grupos certos antes de ver o
     * tamanho final.
     *
     * @param raw valor a formatar, com ou sem máscara; {@code null} retorna {@code ""}.
     * @return RG ou CPF formatado, ou {@code ""} se {@code raw} for nulo/vazio/sem dígitos.
     */
    public static String formatRgCpf(String raw) {
        if (raw == null) return "";
        String numeric = raw.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) return "";
        if (numeric.length() <= 9) return formatRg(numeric);
        return formatCpf(numeric);
    }

    /**
     * Aplica a máscara de RG: {@code XX.XXX.XXX-X}.
     *
     * @param numeric dígitos do RG, já sanitizados (chamado internamente por
     *                {@link #formatRgCpf}); {@code null}/vazio retorna {@code ""}.
     * @return RG formatado.
     */
    private static String formatRg(String numeric) {
        if (numeric == null || numeric.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = numeric.length();

        if (len <= 2) {
            sb.append(numeric);
        } else if (len <= 5) {
            sb.append(numeric, 0, 2).append(".").append(numeric.substring(2));
        } else if (len <= 8) {
            sb.append(numeric, 0, 2).append(".").append(numeric, 2, 5).append(".").append(numeric.substring(5));
        } else {
            sb.append(numeric, 0, 2).append(".")
                    .append(numeric, 2, 5).append(".")
                    .append(numeric, 5, 8).append("-")
                    .append(numeric.substring(8));
        }

        return sb.toString();
    }
}