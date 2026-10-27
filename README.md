# pack-utilities

Biblioteca Java modular (`pack.utilities`) com utilitários para dados usados em aplicações brasileiras. Requer JDK 21 para compilar e usar o artefato. O projeto usa Gradle Wrapper; não é necessário instalar Gradle separadamente.

## Funcionalidades

- `FormatterPack`: máscaras de telefone, CEP, CPF, CNPJ e campos combinados RG/CPF e CPF/CNPJ, inclusive para entradas parciais.
- `ValidatorPack`: validação de e-mail, CEP, telefone, CPF e CNPJ; também oferece validação de documento RG/CPF opcional.
- `DatePack`: conversão entre datas e milissegundos Unix e formatação de datas no padrão brasileiro. As sobrecargas sem `ZoneId` usam o fuso horário padrão da JVM.
- `CurrencyPack`: formatação de valores em reais e conversão de centavos representados como texto para `BigDecimal`.

## Compilação e uso local

Na raiz do repositório, em um ambiente com JDK 21:

```sh
./gradlew build publishToMavenLocal
```

No Windows (PowerShell), use `.\gradlew.bat build publishToMavenLocal`.

Em um projeto consumidor que usa Gradle, declare `mavenLocal()` nos repositórios (antes dos remotos) e a dependência:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("pack.utilities:pack-utilities:1.0.0")
}
```

Se o consumidor usa JPMS, inclua `requires pack.utilities;` em seu `module-info.java`. O artefato publicado localmente contém `module-info.class` e não depende do JitPack. Após republicar a mesma versão, execute novamente o build do consumidor.

Exemplo de uso em Java:

```java
import pack.utilities.CurrencyPack;
import pack.utilities.FormatterPack;
import pack.utilities.ValidatorPack;

String cep = FormatterPack.formatCep("01310100"); // "01310-100"
boolean valido = ValidatorPack.isValidCep(cep);  // true
String reais = CurrencyPack.toBRLCurrency("10.00");
```

## Licença

Este repositório não contém um arquivo de licença. As condições de uso e distribuição precisam ser definidas pelos responsáveis pelo projeto.
