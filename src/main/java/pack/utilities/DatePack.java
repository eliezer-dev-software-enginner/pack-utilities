package pack.utilities;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Utilitário de conversão e formatação de datas/horários, com foco em dois grupos de métodos:
 *
 * <ul>
 *   <li><b>Conversão</b> ({@code long}/{@code LocalDate}/{@code LocalDateTime}): parâmetros de
 *       objeto ({@code LocalDate}, {@code LocalDateTime}, {@code ZoneId}) são obrigatórios — um
 *       {@code null} lança {@link NullPointerException} imediatamente, com mensagem indicando
 *       qual parâmetro faltou, em vez de estourar mais adiante dentro do {@code java.time};</li>
 *   <li><b>Formatação para exibição</b> ({@code String}, no padrão brasileiro): tolerantes a
 *       entrada ausente — {@code null} (ou timestamp {@code 0}, tratado como "não definido")
 *       retornam {@code ""}, no mesmo espírito do {@link FormatterPack}.</li>
 * </ul>
 */
public final class DatePack {

    private static final DateTimeFormatter BR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

    private static final DateTimeFormatter BR_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));

    private DatePack() {
        // Classe utilitária: não deve ser instanciada.
    }

    /**
     * Converte um {@link LocalDate} (início do dia) para milissegundos desde a época Unix,
     * no fuso horário informado.
     *
     * @param data   data a converter; não pode ser {@code null}.
     * @param zoneId fuso horário a considerar; não pode ser {@code null}.
     * @return milissegundos desde a época Unix correspondentes ao início do dia.
     * @throws NullPointerException se {@code data} ou {@code zoneId} forem {@code null}.
     */
    public static long localDateParaMillis(LocalDate data, ZoneId zoneId) {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(zoneId, "zoneId não pode ser nulo");
        return data.atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli();
    }

    /**
     * Equivalente a {@link #localDateParaMillis(LocalDate, ZoneId)} usando o fuso horário
     * padrão da JVM ({@link ZoneId#systemDefault()}).
     *
     * @param data data a converter; não pode ser {@code null}.
     * @return milissegundos desde a época Unix correspondentes ao início do dia.
     * @throws NullPointerException se {@code data} for {@code null}.
     */
    public static long localDateParaMillis(LocalDate data) {
        return localDateParaMillis(data, ZoneId.systemDefault());
    }

    /**
     * Converte milissegundos desde a época Unix para {@link LocalDate}, no fuso horário
     * informado.
     *
     * @param millis milissegundos desde a época Unix.
     * @param zoneId fuso horário a considerar; não pode ser {@code null}.
     * @return a data correspondente.
     * @throws NullPointerException se {@code zoneId} for {@code null}.
     */
    public static LocalDate millisParaLocalDate(long millis, ZoneId zoneId) {
        Objects.requireNonNull(zoneId, "zoneId não pode ser nulo");
        return Instant.ofEpochMilli(millis)
                .atZone(zoneId)
                .toLocalDate();
    }

    /**
     * Equivalente a {@link #millisParaLocalDate(long, ZoneId)} usando o fuso horário padrão
     * da JVM ({@link ZoneId#systemDefault()}).
     *
     * @param millis milissegundos desde a época Unix.
     * @return a data correspondente.
     */
    public static LocalDate millisParaLocalDate(long millis) {
        return millisParaLocalDate(millis, ZoneId.systemDefault());
    }

    /**
     * Converte um {@link LocalDateTime} para milissegundos desde a época Unix, no fuso
     * horário informado.
     *
     * @param dataHora data/hora a converter; não pode ser {@code null}.
     * @param zoneId   fuso horário a considerar; não pode ser {@code null}.
     * @return milissegundos desde a época Unix correspondentes.
     * @throws NullPointerException se {@code dataHora} ou {@code zoneId} forem {@code null}.
     */
    public static long localDateTimeParaMillis(LocalDateTime dataHora, ZoneId zoneId) {
        Objects.requireNonNull(dataHora, "dataHora não pode ser nula");
        Objects.requireNonNull(zoneId, "zoneId não pode ser nulo");
        return dataHora.atZone(zoneId)
                .toInstant()
                .toEpochMilli();
    }

    /**
     * Equivalente a {@link #localDateTimeParaMillis(LocalDateTime, ZoneId)} usando o fuso
     * horário padrão da JVM ({@link ZoneId#systemDefault()}).
     *
     * @param dataHora data/hora a converter; não pode ser {@code null}.
     * @return milissegundos desde a época Unix correspondentes.
     * @throws NullPointerException se {@code dataHora} for {@code null}.
     */
    public static long localDateTimeParaMillis(LocalDateTime dataHora) {
        return localDateTimeParaMillis(dataHora, ZoneId.systemDefault());
    }

    /**
     * Formata um timestamp (millis desde a época Unix) como data/hora no padrão brasileiro
     * {@code dd/MM/yyyy HH:mm}, usando o fuso horário padrão da JVM.
     *
     * @param timestamp milissegundos desde a época Unix; {@code null} ou {@code 0} são
     *                  tratados como "não definido".
     * @return data/hora formatada, ou {@code ""} se {@code timestamp} for nulo ou zero.
     */
    public static String millisToBrazilianDateTime(Long timestamp) {
        if (timestamp == null || timestamp == 0) return "";

        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(BR_DATE_TIME_FORMATTER);
    }

    /**
     * Formata um {@link LocalDateTime} no padrão brasileiro {@code dd/MM/yyyy HH:mm}, usando
     * o fuso horário padrão da JVM.
     *
     * @param dataHora data/hora a formatar; pode ser {@code null}.
     * @return data/hora formatada, ou {@code ""} se {@code dataHora} for {@code null}.
     */
    public static String localDateTimeToBrazilianDateTime(LocalDateTime dataHora) {
        if (dataHora == null) return "";
        long millis = localDateTimeParaMillis(dataHora);
        return millisToBrazilianDateTime(millis);
    }

    /**
     * Formata um timestamp (millis desde a época Unix) como data no padrão brasileiro
     * {@code dd/MM/yyyy}, usando o fuso horário padrão da JVM.
     *
     * @param timestamp milissegundos desde a época Unix; {@code null} ou {@code 0} são
     *                  tratados como "não definido".
     * @return data formatada, ou {@code ""} se {@code timestamp} for nulo ou zero.
     */
    public static String millisToBrazilianDate(Long timestamp) {
        if (timestamp == null || timestamp == 0) return "";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(BR_DATE_FORMATTER);
    }
}

// Uso:
// long millis = DatePack.localDateParaMillis(LocalDate.now());