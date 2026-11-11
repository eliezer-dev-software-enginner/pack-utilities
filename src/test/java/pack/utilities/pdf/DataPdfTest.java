package pack.utilities.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPdfTest {
    @TempDir Path directory;

    @Test
    void rendersJsoncDataRepeatsConditionsTablesAndPagination() throws Exception {
        String template = """
                {
                  // Layout belongs to data, not to Java coordinates.
                  "page": {"size": "A4", "orientation": "PORTRAIT", "margin": 36},
                  "fonts": {
                    "regular": "/pack/utilities/pdf/fonts/DejaVuSans.ttf",
                    "bold": "/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf"
                  },
                  "pageHeader": [
                    {"type": "TEXT", "value": "${title}", "fontSize": 12, "bold": true}
                  ],
                  "body": [
                    {"type": "REPEAT", "path": "groups", "body": [
                      {"type": "TEXT", "value": "${name}", "fontSize": 10},
                      {"type": "TABLE", "rowsPath": "rows", "rowHeight": 20,
                       "columns": [
                         {"header": "Item", "value": "${item}", "weight": 3},
                         {"header": "Valor", "value": "${value}", "weight": 1}
                       ]}
                    ]},
                    {"type": "IF", "path": "showFooter", "body": [
                      {"type": "TEXT", "value": "${footer}", "fontSize": 10}
                    ]}
                  ]
                }
                """;
        List<Map<String, String>> rows = java.util.stream.IntStream.rangeClosed(1, 70)
                .mapToObj(index -> Map.of("item", "Produto " + index, "value", "R$ " + index))
                .toList();
        Map<String, Object> data = Map.of(
                "title", "Pedido orientado a dados",
                "groups", List.of(Map.of("name", "Grupo principal", "rows", rows)),
                "showFooter", true,
                "footer", "Total calculado"
        );
        Path output = directory.resolve("data.pdf");

        DataPdf.write(output, new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)), data);

        try (PDDocument document = PDDocument.load(output.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(document.getNumberOfPages() > 1);
            assertEquals(document.getNumberOfPages(), text.split("Pedido orientado a dados", -1).length - 1);
            assertTrue(text.contains("Produto 70"));
            assertTrue(text.contains("Total calculado"));
        }
    }
}
