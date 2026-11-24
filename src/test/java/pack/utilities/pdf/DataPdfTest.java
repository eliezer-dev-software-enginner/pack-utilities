package pack.utilities.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                         {"header": "${itemHeader}", "value": "${item}", "weight": 3},
                         {"header": "${valueHeader}", "value": "${value}", "weight": 1}
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
                "groups", List.of(Map.of("name", "Grupo principal", "itemHeader", "Item", "rows", rows)),
                "valueHeader", "Valor",
                "showFooter", true,
                "footer", "Total calculado"
        );
        Path output = directory.resolve("data.pdf");

        DataPdf.write(output, new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)), data);

        try (PDDocument document = PDDocument.load(output.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(document.getNumberOfPages() > 1);
            assertEquals(document.getNumberOfPages(), text.split("Pedido orientado a dados", -1).length - 1);
            assertTrue(text.contains("Item"));
            assertTrue(text.contains("Valor"));
            assertFalse(text.contains("${itemHeader}"));
            assertFalse(text.contains("${valueHeader}"));
            assertTrue(text.contains("Produto 70"));
            assertTrue(text.contains("Total calculado"));
        }
    }

    @Test
    void rendersColumnsBandsAndBrandedTableStyles() throws Exception {
        String template = """
                {
                  "page": {"size": "A4", "orientation": "PORTRAIT", "margin": 36},
                  "fonts": {
                    "regular": "/pack/utilities/pdf/fonts/DejaVuSans.ttf",
                    "bold": "/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf"
                  },
                  "body": [
                    {"type": "COLUMNS", "gap": 18, "minHeight": 38, "columns": [
                      {"weight": 2, "body": [
                        {"type": "TEXT", "value": "${company}", "fontSize": 16, "bold": true, "color": "#1F4E89"}
                      ]},
                      {"weight": 1, "body": [
                        {"type": "TEXT", "value": "PEDIDO", "fontSize": 14, "bold": true, "align": "RIGHT", "color": "#1F4E89"},
                        {"type": "TEXT", "value": "${date}", "fontSize": 8, "align": "RIGHT"}
                      ]}
                    ]},
                    {"type": "BAND", "value": "ITENS DO PEDIDO", "backgroundColor": "#1F4E89", "color": "#FFFFFF"},
                    {"type": "TABLE", "rowsPath": "items", "headerBackgroundColor": "#1F4E89",
                     "headerTextColor": "#FFFFFF", "borderColor": "#D4DCE8", "alternateRowColor": "#F4F7FB",
                     "columns": [
                       {"header": "Descrição", "value": "${description}", "weight": 3},
                       {"header": "Total", "value": "${total}", "weight": 1, "align": "RIGHT"}
                     ]}
                  ]
                }
                """;
        Map<String, Object> data = Map.of(
                "company", "Minastex",
                "date", "23 de novembro de 2026",
                "items", List.of(Map.of("description", "Painel MDF", "total", "R$ 123,45")));
        Path output = directory.resolve("branded.pdf");

        DataPdf.write(output, new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)), data);

        try (PDDocument document = PDDocument.load(output.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Minastex"));
            assertTrue(text.contains("PEDIDO"));
            assertTrue(text.contains("ITENS DO PEDIDO"));
            assertTrue(text.contains("Painel MDF"));
            BufferedImage page = new PDFRenderer(document).renderImageWithDPI(0, 72);
            long bluePixels = java.util.stream.IntStream.range(0, page.getHeight())
                    .mapToLong(y -> java.util.stream.IntStream.range(0, page.getWidth())
                            .filter(x -> {
                                int rgb = page.getRGB(x, y);
                                int r = rgb >> 16 & 0xff, g = rgb >> 8 & 0xff, b = rgb & 0xff;
                                return b > r + 25 && b > g + 10;
                            }).count()).sum();
            assertTrue(bluePixels > 1_000, "As faixas e o cabeçalho devem preservar a identidade azul");
        }
    }

    @Test
    void shrinkBandPreservesCompleteValueInsteadOfEllipsis() throws Exception {
        String template = """
                {
                  "page": {"size": "A4", "orientation": "PORTRAIT", "margin": 220},
                  "fonts": {
                    "regular": "/pack/utilities/pdf/fonts/DejaVuSans.ttf",
                    "bold": "/pack/utilities/pdf/fonts/DejaVuSans-Bold.ttf"
                  },
                  "body": [
                    {"type": "BAND", "value": "TOTAL DO PEDIDO   ${total}",
                     "fontSize": 12, "fitMode": "SHRINK"}
                  ]
                }
                """;
        Path output = directory.resolve("complete-total.pdf");

        DataPdf.write(output, new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)),
                Map.of("total", "R$ 173.252,31"));

        try (PDDocument document = PDDocument.load(output.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("TOTAL DO PEDIDO   R$ 173.252,31"));
            assertFalse(text.contains("…"));
        }
    }
}
