# DataPdf JSONC template reference

`DataPdf` renders a PDF from two independent inputs:

1. a JSONC template that owns the document layout; and
2. an arbitrary Java object that owns the document data.

The renderer is intentionally data-driven. Application code maps its domain to
plain objects, records, maps, and lists; the template controls pages, fonts,
text, images, rules, conditional sections, repeated sections, and tables.

```java
import pack.utilities.pdf.DataPdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class InvoicePdf {
    private static final String TEMPLATE = "/pdf/invoice.jsonc";

    static void write(Path destination, Invoice invoice) throws IOException {
        Map<String, Object> data = Map.of(
                "title", "Invoice #" + invoice.number(),
                "customer", Map.of("name", invoice.customerName()),
                "items", invoice.items().stream().map(item -> Map.of(
                        "description", item.description(),
                        "quantity", item.quantity().toPlainString(),
                        "total", item.formattedTotal()
                )).toList(),
                "notes", invoice.notes(),
                "showNotes", !invoice.notes().isBlank()
        );

        try (InputStream template = InvoicePdf.class.getResourceAsStream(TEMPLATE)) {
            if (template == null) throw new IOException("Missing template: " + TEMPLATE);
            DataPdf.write(destination, template, data);
        }
    }
}
```

`DataPdf.write` creates missing parent directories, creates one PDF document per
call, and closes all resources that it opens. The caller owns and must close the
template `InputStream`.

## JSONC syntax

Templates accept standard JSON plus:

- JavaScript-style line comments: `// comment`;
- JavaScript-style block comments: `/* comment */`; and
- trailing commas.

The parser does not document support for YAML comments or unquoted property
names. Use quoted JSON property names and values.

## Complete template structure

```jsonc
{
  "page": {
    "size": "A4",
    "orientation": "PORTRAIT",
    "margin": 42
  },
  "fonts": {
    "regular": "/pack/utilities/pdf/fonts/DejaVuSans.ttf",
    "bold": "/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf"
  },
  "pageHeader": [
    {
      "type": "TEXT",
      "value": "${title}",
      "fontSize": 11,
      "bold": true,
      "align": "RIGHT",
      "spacingAfter": 8
    }
  ],
  "body": [
    {
      "type": "TEXT",
      "value": "Customer: ${customer.name}",
      "fontSize": 16,
      "bold": true,
      "spacingAfter": 10
    },
    {
      "type": "TABLE",
      "rowsPath": "items",
      "rowHeight": 22,
      "fontSize": 9,
      "headerFontSize": 9,
      "cellPadding": 4,
      "spacingAfter": 12,
      "columns": [
        { "header": "Description", "value": "${description}", "weight": 4 },
        { "header": "Quantity", "value": "${quantity}", "weight": 1 },
        { "header": "Total", "value": "${total}", "weight": 1 }
      ]
    },
    {
      "type": "IF",
      "path": "showNotes",
      "body": [
        { "type": "LINE" },
        { "type": "TEXT", "value": "${notes}" }
      ]
    }
  ]
}
```

The root properties `page`, `fonts`, and `body` are required. `pageHeader` is
optional.

## Root properties

### `page`

| Property | Type | Default | Accepted values and behavior |
| --- | --- | --- | --- |
| `size` | string | `A4` | `A3`, `A4`, or `LETTER`, case-insensitive. Any other value currently falls back to `A4`. |
| `orientation` | string | portrait | `LANDSCAPE`, case-insensitive, rotates the selected page size. Any other value uses portrait orientation. |
| `margin` | number | `42` | One margin, in PDF points, applied to all four sides. |

One PDF point is 1/72 inch. The writable width is the page width minus twice the
margin.

### `fonts`

| Property | Type | Required | Behavior |
| --- | --- | --- | --- |
| `regular` | string | yes | Classpath resource used for regular text. |
| `bold` | string | no | Classpath resource used for bold text. Defaults to `regular`. |

Fonts are loaded as embedded TrueType/OpenType resources through PDFBox. Resource
lookup first uses the current thread context class loader and then the
`DataPdf` class loader. The library includes:

- `/pack/utilities/pdf/fonts/DejaVuSans.ttf`
- `/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf`

Use a font containing every glyph that the document may render. A missing font
resource fails generation with `IOException`.

### `pageHeader`

`pageHeader` is an optional array of normal elements. It is rendered at the top
of every page, including pages created by automatic table or content pagination.
Expressions in a page header always resolve against the root data object.

Keep the header shorter than the available page height. Page breaks are disabled
while the header itself is being rendered, so an oversized header is not split.

### `body`

`body` is the required array of document elements, rendered from top to bottom.
Elements may cause automatic page breaks when their required height would cross
the bottom margin.

## Data paths and expressions

An expression has the form `${path.to.value}`:

```jsonc
{ "type": "TEXT", "value": "Customer: ${customer.name}" }
```

Paths use dots to descend through object properties or map keys. Array indexes
are not a documented path feature. The special path `.` resolves to the current
context when used as an element path.

During `REPEAT` and table row rendering, lookup follows this order:

1. the current repeated item or table row; then
2. the root data object when that path is missing locally.

This permits a repeated item to combine local fields with root fields:

```jsonc
{
  "type": "REPEAT",
  "path": "items",
  "body": [
    { "type": "TEXT", "value": "${description} — ${company.currency}" }
  ]
}
```

Only scalar JSON values are inserted into text. Missing paths, objects, arrays,
and `null` interpolate as an empty string. Java data is converted to a Jackson
JSON tree, so maps, records, JavaBeans, lists, booleans, strings, numbers, and
other Jackson-compatible objects can be supplied.

## Element reference

Every element requires `type`. Type names are case-insensitive. Supported types
are `TEXT`, `IMAGE`, `LINE`, `SPACER`, `TABLE`, `REPEAT`, and `IF`. An unknown or
missing type throws `IllegalArgumentException`.

### `TEXT`

Renders interpolated text and wraps it to the writable page width.

```jsonc
{
  "type": "TEXT",
  "value": "Invoice ${number}",
  "fontSize": 14,
  "bold": true,
  "align": "CENTER",
  "spacingAfter": 10
}
```

| Property | Type | Default | Behavior |
| --- | --- | --- | --- |
| `value` | string | `""` | Text with optional `${...}` expressions. |
| `fontSize` | number | `10` | Font size in points. |
| `bold` | boolean | `false` | Uses the configured bold font. |
| `align` | string | `LEFT` | `LEFT`, `CENTER`, or `RIGHT`, case-insensitive. Unknown values use left alignment. |
| `spacingAfter` | number | `7` | Vertical space in points after the final rendered line. |

Carriage returns and line feeds are converted to spaces. Remaining whitespace is
normalized while wrapping. Explicit multiline text is not preserved.

### `IMAGE`

Renders an image loaded from a local filesystem path, with an optional classpath
fallback.

```jsonc
{
  "type": "IMAGE",
  "path": "${company.logoPath}",
  "fallbackResource": "/assets/default-logo.png",
  "maxWidth": 120,
  "maxHeight": 60,
  "align": "CENTER",
  "spacingAfter": 8
}
```

| Property | Type | Default | Behavior |
| --- | --- | --- | --- |
| `path` | string | `""` | Interpolated local filesystem path. |
| `fallbackResource` | string | `""` | Literal classpath resource used when `path` is empty or is not a regular file. This property is not interpolated. |
| `maxWidth` | number | `120` | Maximum rendered width in points. |
| `maxHeight` | number | `60` | Maximum rendered height in points. |
| `align` | string | `LEFT` | `LEFT`, `CENTER`, or `RIGHT`. |
| `spacingAfter` | number | `8` | Vertical space after the image. |

The aspect ratio is preserved. The current scale calculation may enlarge an
image when both source dimensions are below the configured maxima. HTTP URLs,
data URLs, and `file:` URIs are not fetched or decoded; pass a filesystem path.
If neither the path nor fallback yields bytes, the element renders nothing.

### `LINE`

Draws a horizontal rule across the writable page width.

```jsonc
{ "type": "LINE", "width": 0.75, "spacingAfter": 10 }
```

| Property | Type | Default | Behavior |
| --- | --- | --- | --- |
| `width` | number | `0.5` | Stroke width in points. |
| `spacingAfter` | number | `8` | Vertical space after the rule. |

### `SPACER`

Consumes vertical space.

```jsonc
{ "type": "SPACER", "height": 16 }
```

| Property | Type | Default | Behavior |
| --- | --- | --- | --- |
| `height` | number | `12` | Vertical space in points. |

### `TABLE`

Renders a fixed-height header row followed by zero or more data rows.

```jsonc
{
  "type": "TABLE",
  "rowsPath": "items",
  "rowHeight": 22,
  "fontSize": 9,
  "headerFontSize": 9,
  "cellPadding": 4,
  "spacingAfter": 12,
  "columns": [
    { "header": "Item", "value": "${name}", "weight": 3 },
    { "header": "Value", "value": "${formattedValue}", "weight": 1 }
  ]
}
```

Table properties:

| Property | Type | Default | Behavior |
| --- | --- | --- | --- |
| `rowsPath` | string | `""` | Path to an array, resolved against the current context and then the root. A non-array renders no table. |
| `columns` | array | none | Column definitions. An absent or empty array renders no table. |
| `rowHeight` | number | `22` | Fixed height for header and data rows. |
| `fontSize` | number | `9` | Data-cell font size. |
| `headerFontSize` | number | `fontSize` | Header font size. |
| `cellPadding` | number | `4` | Horizontal cell padding. |
| `spacingAfter` | number | `0` | Vertical space after the table. |

Column properties:

| Property | Type | Default | Behavior |
| --- | --- | --- | --- |
| `header` | string | `""` | Literal header text. Headers are not expression-interpolated in the current version. |
| `value` | string | `""` | Cell text with expressions resolved against the row, then root data. |
| `weight` | number | `1` | Relative share of the writable table width. |

Column widths are proportional. For weights `3` and `1`, the first column uses
75% and the second uses 25% of the writable width.

Headers use the bold font and a fixed light-gray background. Table borders and
text colors are currently fixed by the renderer. Each row has a fixed height;
cell text does not wrap and is truncated with `…` when it exceeds the available
width. Newlines in cell values are converted to spaces.

When the next row would cross the bottom margin, `DataPdf` starts a new page,
renders `pageHeader`, and repeats the table header before continuing the rows.

### `REPEAT`

Renders a nested body once for every item in an array.

```jsonc
{
  "type": "REPEAT",
  "path": "groups",
  "body": [
    { "type": "TEXT", "value": "${name}", "bold": true },
    { "type": "TABLE", "rowsPath": "items", "columns": [
      { "header": "Item", "value": "${description}" }
    ]}
  ]
}
```

| Property | Type | Required | Behavior |
| --- | --- | --- | --- |
| `path` | string | yes | Path to an array. A missing or non-array value renders nothing. |
| `body` | array | yes | Elements rendered with each array item as the current context. |

Nested `REPEAT`, `IF`, and `TABLE` elements are supported.

### `IF`

Conditionally renders a nested body.

```jsonc
{
  "type": "IF",
  "path": "status",
  "equals": "PAID",
  "body": [
    { "type": "TEXT", "value": "Paid", "bold": true }
  ]
}
```

| Property | Type | Required | Behavior |
| --- | --- | --- | --- |
| `path` | string | yes | Value tested against the current context, then root data. |
| `equals` | string | no | When present, visibility is an exact, case-sensitive comparison with the value converted to text. |
| `body` | array | yes | Elements rendered when the condition is true. |

Without `equals`, truthiness is:

- boolean: `true` is visible;
- number: any non-zero value is visible;
- string: any non-blank value is visible;
- missing or `null`: hidden;
- object or array: currently hidden because they do not produce scalar text.

Use an explicit boolean such as `showBankDetails` when testing whether a
collection or object should appear.

## Pagination behavior

The renderer maintains one vertical cursor and creates a new page when the next
element would cross the bottom margin.

- `TEXT` estimates the complete wrapped block before rendering it.
- `IMAGE` estimates the scaled image height.
- `SPACER` and `LINE` check their required height.
- `TABLE` paginates between rows and repeats its header.
- `pageHeader` is rendered on every page before body content resumes.

A single text block, image, spacer, or table row taller than the writable page
height is not split internally. Keep individual elements within the page area.

## Resource and output behavior

- `destination` and `template` must be non-null.
- Missing destination parent directories are created automatically.
- Fonts and fallback images are loaded from the classpath.
- The output file is replaced by PDFBox when generation succeeds.
- Each call creates an independent `PDDocument`; do not mutate input data while
  another thread is serializing it.
- PDF generation performs file I/O and font/image processing. Desktop
  applications should run it outside the UI thread and return completion or
  failure messages on the UI dispatcher.

## Validation and errors

The renderer performs structural validation for the three required root fields:

- missing or non-object `page` → `IllegalArgumentException`;
- missing or non-object `fonts` → `IllegalArgumentException`;
- missing or non-array `body` → `IllegalArgumentException`.

Other relevant failures include:

- unknown element type → `IllegalArgumentException`;
- invalid JSONC or unreadable template → `IOException` from Jackson;
- missing/blank font or fallback resource → `IOException`;
- invalid or unsupported image bytes → `IOException` from PDFBox;
- unwritable destination → `IOException`.

Some optional structures intentionally render nothing instead of failing, such
as a `REPEAT` with a non-array path, a table with no rows array, or an image with
neither a valid file nor fallback.

## Current limitations

The current JSONC API does not provide:

- page footers or page-number expressions;
- arbitrary text, fill, border, or table-header colors;
- per-side margins or padding;
- hyperlinks or clickable buttons;
- remote image downloading or data-URL decoding;
- explicit page-break elements;
- row/column spanning;
- wrapped or variable-height table cells;
- interpolated table header labels;
- preservation of explicit line breaks in `TEXT`; or
- a schema file that editors can use for completion.

Treat these as unsupported capabilities rather than undocumented properties.
Extend `DataPdf` and its tests before publishing templates that depend on them.

## Full example

```jsonc
{
  // src/main/resources/pdf/invoice.jsonc
  "page": { "size": "A4", "orientation": "PORTRAIT", "margin": 42 },
  "fonts": {
    "regular": "/pack/utilities/pdf/fonts/DejaVuSans.ttf",
    "bold": "/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf"
  },
  "pageHeader": [
    { "type": "TEXT", "value": "${title}", "fontSize": 10, "bold": true, "align": "RIGHT" },
    { "type": "LINE", "spacingAfter": 10 }
  ],
  "body": [
    {
      "type": "IMAGE",
      "path": "${company.logoPath}",
      "fallbackResource": "/assets/default-logo.png",
      "maxWidth": 120,
      "maxHeight": 60,
      "spacingAfter": 8
    },
    { "type": "TEXT", "value": "${company.name}", "fontSize": 18, "bold": true, "spacingAfter": 3 },
    { "type": "TEXT", "value": "Customer: ${customer.name}", "fontSize": 11, "spacingAfter": 8 },
    { "type": "LINE", "spacingAfter": 12 },
    {
      "type": "REPEAT",
      "path": "groups",
      "body": [
        { "type": "TEXT", "value": "${name}", "fontSize": 12, "bold": true, "spacingAfter": 4 },
        {
          "type": "TABLE",
          "rowsPath": "items",
          "rowHeight": 22,
          "fontSize": 9,
          "cellPadding": 4,
          "spacingAfter": 12,
          "columns": [
            { "header": "Description", "value": "${description}", "weight": 4 },
            { "header": "Qty.", "value": "${quantity}", "weight": 1 },
            { "header": "Value", "value": "${value}", "weight": 1 }
          ]
        }
      ]
    },
    { "type": "SPACER", "height": 6 },
    { "type": "TEXT", "value": "Total: ${total}", "fontSize": 16, "bold": true, "align": "RIGHT" },
    {
      "type": "IF",
      "path": "showNotes",
      "body": [
        { "type": "SPACER", "height": 8 },
        { "type": "TEXT", "value": "Notes", "fontSize": 10, "bold": true, "spacingAfter": 3 },
        { "type": "TEXT", "value": "${notes}", "fontSize": 9 }
      ]
    },
  ]
}
```

For an executable pagination example, see
[`DataPdfTest`](../src/test/java/pack/utilities/pdf/DataPdfTest.java).
