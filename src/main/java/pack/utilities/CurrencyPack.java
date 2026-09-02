package pack.utilities;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

//TODO: não mexer ainda
public class CurrencyPack {
    public static String toBRLCurrency(BigDecimal value){
        final NumberFormat BRL =
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return BRL.format(value).replace('\u00a0', ' ');
    }

    public static String toBRLCurrency(String value){
        return toBRLCurrency(new BigDecimal(value));
    }


    /**
     * Esse método é usado para transformar os centavos visuais para valor em Real que será persistido no banco de dados.
     * 1000 centavos equivalem a 10 reais.
     * A conversão entre centavos e reais é baseada na relação de que 1 real = 100 centavos.  Para converter centavos em reais, basta dividir o número de centavos por 100:
     *
     * 1000 centavos ÷ 100 = 10 reais
     * @param centavos
     * @return
     */
public static BigDecimal deCentavosParaReal(String centavos){
        if (centavos == null || centavos.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(centavos).movePointLeft(2);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
