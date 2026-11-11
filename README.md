# pack-utilities

Modular Java library (`pack.utilities`) with reusable formatting, validation, date,
currency, and data-driven PDF tools. Building and consuming the artifact requires JDK 21.
The repository includes the Gradle Wrapper, so a separate Gradle installation is unnecessary.

## Features

- `FormatterPack`: phone, postal code, CPF, CNPJ, RG/CPF, and CPF/CNPJ masks, including partial input.
- `ValidatorPack`: e-mail, postal code, phone, CPF, CNPJ, and optional RG/CPF validation.
- `DatePack`: Brazilian date formatting and conversions between dates and Unix milliseconds.
  Overloads without a `ZoneId` use the JVM default time zone.
- `CurrencyPack`: Brazilian real formatting and conversion of cent strings to `BigDecimal`.
- `DataPdf`: JSONC-template PDF generation with text, images, rules, conditions, repetition,
  paginated tables, and repeated page headers.

## Local build and usage

From the repository root, with JDK 21:

```sh
./gradlew build publishToMavenLocal
```

On Windows PowerShell, use `./gradlew.bat build publishToMavenLocal`.

Add Maven Local before remote repositories and declare the dependency:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("pack.utilities:pack-utilities:1.0.0")
}
```

JPMS consumers must add `requires pack.utilities;` to `module-info.java`. The locally
published artifact contains `module-info.class` and does not depend on JitPack. Rebuild a
consumer after publishing another build with the same version.

```java
import pack.utilities.CurrencyPack;
import pack.utilities.FormatterPack;
import pack.utilities.ValidatorPack;

String postalCode = FormatterPack.formatCep("01310100"); // "01310-100"
boolean valid = ValidatorPack.isValidCep(postalCode);    // true
String reais = CurrencyPack.toBRLCurrency("10.00");
```

## Data-driven PDF

Applications keep document layout in a JSONC resource and pass only the document data:

```java
import pack.utilities.pdf.DataPdf;

try (var template = MyDocument.class.getResourceAsStream("/pdf/order.jsonc")) {
    DataPdf.write(destination, template, Map.of(
            "title", "Order #42",
            "items", List.of(Map.of("name", "Panel", "value", "$50.00"))
    ));
}
```

Templates use `${field}` expressions in `TEXT`, `IMAGE`, and `TABLE` cells. `REPEAT`
iterates over lists, `IF` controls optional blocks, and `pageHeader` is rendered again on
every page. The library ships DejaVu Sans regular and bold at:

- `/pack/utilities/pdf/fonts/DejaVuSans.ttf`
- `/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf`

See [`DataPdfTest`](src/test/java/pack/utilities/pdf/DataPdfTest.java) for an executable
template containing repetition, conditional content, pagination, and a table.

## License

This repository does not currently include a project license. The bundled DejaVu fonts retain
their own license in
[`src/main/resources/pack/utilities/pdf/fonts/LICENSE.txt`](src/main/resources/pack/utilities/pdf/fonts/LICENSE.txt).
Project usage and distribution terms must be defined by the maintainers.
