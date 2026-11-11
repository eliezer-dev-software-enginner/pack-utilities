package pack.utilities.pdf;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders PDF documents from JSONC layout templates and arbitrary object data. */
public final class DataPdf {
    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{([^}]+)}");
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    private DataPdf() {
    }

    /**
     * Writes a document without exposing PDFBox to the application.
     * The caller owns and closes {@code template}; fonts and images are loaded from the context classpath.
     */
    public static void write(Path destination, InputStream template, Object data) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(template, "template");
        JsonNode definition = MAPPER.readTree(template);
        JsonNode root = MAPPER.valueToTree(data);
        validate(definition);
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (PDDocument document = new PDDocument();
             InputStream regularStream = resource(text(definition.path("fonts"), "regular", null));
             InputStream boldStream = resource(text(definition.path("fonts"), "bold",
                     text(definition.path("fonts"), "regular", null)))) {
            PDFont regular = PDType0Font.load(document, regularStream);
            PDFont bold = PDType0Font.load(document, boldStream);
            new Renderer(document, definition, root, regular, bold).render();
            document.save(destination.toFile());
        }
    }

    private static void validate(JsonNode definition) {
        if (!definition.path("page").isObject()) throw new IllegalArgumentException("Template PDF sem 'page'");
        if (!definition.path("fonts").isObject()) throw new IllegalArgumentException("Template PDF sem 'fonts'");
        if (!definition.path("body").isArray()) throw new IllegalArgumentException("Template PDF sem 'body'");
    }

    private static InputStream resource(String path) throws IOException {
        if (path == null || path.isBlank()) throw new IOException("Caminho de recurso PDF vazio");
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(normalized);
        if (stream == null) stream = DataPdf.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
        if (stream == null) throw new IOException("Recurso PDF não encontrado: " + path);
        return stream;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : fallback;
    }

    private static final class Renderer {
        private final PDDocument document;
        private final JsonNode template;
        private final JsonNode root;
        private final PDFont regular;
        private final PDFont bold;
        private final PDRectangle pageSize;
        private final float margin;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private boolean drawingPageHeader;

        private Renderer(PDDocument document, JsonNode template, JsonNode root, PDFont regular, PDFont bold) {
            this.document = document;
            this.template = template;
            this.root = root;
            this.regular = regular;
            this.bold = bold;
            this.pageSize = pageSize(template.path("page"));
            this.margin = (float) template.path("page").path("margin").asDouble(42);
        }

        private void render() throws IOException {
            try {
                newPage();
                renderElements(template.path("body"), root);
            } finally {
                closeStream();
            }
        }

        private void renderElements(JsonNode elements, JsonNode context) throws IOException {
            if (!elements.isArray()) return;
            for (JsonNode element : elements) {
                String type = element.path("type").asText("").toUpperCase(Locale.ROOT);
                switch (type) {
                    case "TEXT" -> drawText(element, context);
                    case "IMAGE" -> drawImage(element, context);
                    case "LINE" -> drawLine(element);
                    case "SPACER" -> space((float) element.path("height").asDouble(12));
                    case "TABLE" -> drawTable(element, context);
                    case "REPEAT" -> repeat(element, context);
                    case "IF" -> conditional(element, context);
                    default -> throw new IllegalArgumentException("Elemento PDF desconhecido: " + type);
                }
            }
        }

        private void repeat(JsonNode element, JsonNode context) throws IOException {
            JsonNode values = resolveNode(element.path("path").asText(), context);
            if (!values.isArray()) return;
            for (JsonNode value : values) renderElements(element.path("body"), value);
        }

        private void conditional(JsonNode element, JsonNode context) throws IOException {
            JsonNode value = resolveNode(element.path("path").asText(), context);
            boolean visible = value.isBoolean() ? value.asBoolean()
                    : value.isNumber() ? value.asDouble() != 0
                    : !value.isMissingNode() && !value.isNull() && !value.asText().isBlank();
            if (element.has("equals")) visible = element.path("equals").asText().equals(value.asText());
            if (visible) renderElements(element.path("body"), context);
        }

        private void drawText(JsonNode element, JsonNode context) throws IOException {
            float size = (float) element.path("fontSize").asDouble(10);
            float spacing = (float) element.path("spacingAfter").asDouble(7);
            PDFont font = element.path("bold").asBoolean() ? bold : regular;
            String value = interpolate(element.path("value").asText(""), context).replaceAll("[\\r\\n]+", " ");
            List<String> lines = wrap(value, font, size, writableWidth());
            ensure(lines.size() * (size + 3) + spacing);
            for (String line : lines) {
                float width = width(line, font, size);
                float x = switch (element.path("align").asText("LEFT").toUpperCase(Locale.ROOT)) {
                    case "CENTER" -> margin + Math.max(0, (writableWidth() - width) / 2);
                    case "RIGHT" -> margin + Math.max(0, writableWidth() - width);
                    default -> margin;
                };
                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(x, y);
                stream.showText(line);
                stream.endText();
                y -= size + 3;
            }
            y -= spacing;
        }

        private void drawImage(JsonNode element, JsonNode context) throws IOException {
            String path = interpolate(element.path("path").asText(""), context).trim();
            byte[] bytes = null;
            if (!path.isBlank()) {
                Path file = Path.of(path);
                if (Files.isRegularFile(file)) bytes = Files.readAllBytes(file);
            }
            if (bytes == null) {
                String fallback = element.path("fallbackResource").asText("");
                if (!fallback.isBlank()) try (InputStream input = resource(fallback)) { bytes = input.readAllBytes(); }
            }
            if (bytes == null) return;
            PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, "data-pdf-image");
            float maxWidth = (float) element.path("maxWidth").asDouble(120);
            float maxHeight = (float) element.path("maxHeight").asDouble(60);
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            ensure(height + element.path("spacingAfter").asDouble(8));
            float x = switch (element.path("align").asText("LEFT").toUpperCase(Locale.ROOT)) {
                case "CENTER" -> margin + (writableWidth() - width) / 2;
                case "RIGHT" -> page.getMediaBox().getWidth() - margin - width;
                default -> margin;
            };
            stream.drawImage(image, x, y - height, width, height);
            y -= height + (float) element.path("spacingAfter").asDouble(8);
        }

        private void drawLine(JsonNode element) throws IOException {
            ensure(8);
            stream.setLineWidth((float) element.path("width").asDouble(.5));
            stream.moveTo(margin, y);
            stream.lineTo(page.getMediaBox().getWidth() - margin, y);
            stream.stroke();
            y -= (float) element.path("spacingAfter").asDouble(8);
        }

        private void drawTable(JsonNode element, JsonNode context) throws IOException {
            JsonNode columns = element.path("columns");
            JsonNode rows = resolveNode(element.path("rowsPath").asText(), context);
            if (!columns.isArray() || columns.isEmpty() || !rows.isArray()) return;
            float rowHeight = (float) element.path("rowHeight").asDouble(22);
            float fontSize = (float) element.path("fontSize").asDouble(9);
            float headerFontSize = (float) element.path("headerFontSize").asDouble(fontSize);
            float padding = (float) element.path("cellPadding").asDouble(4);
            float[] widths = columnWidths(columns);
            ensure(rowHeight * 2);
            drawTableRow(columns, null, widths, rowHeight, headerFontSize, padding, true);
            for (JsonNode row : rows) {
                if (y - rowHeight < margin) {
                    newPage();
                    drawTableRow(columns, null, widths, rowHeight, headerFontSize, padding, true);
                }
                drawTableRow(columns, row, widths, rowHeight, fontSize, padding, false);
            }
            y -= (float) element.path("spacingAfter").asDouble(0);
        }

        private void drawTableRow(JsonNode columns, JsonNode row, float[] widths, float height,
                                  float fontSize, float padding, boolean header) throws IOException {
            float x = margin;
            float bottom = y - height;
            if (header) {
                stream.setNonStrokingColor(238, 238, 238);
                stream.addRect(x, bottom, writableWidth(), height);
                stream.fill();
                stream.setNonStrokingColor(0, 0, 0);
            }
            for (int index = 0; index < columns.size(); index++) {
                JsonNode column = columns.get(index);
                stream.addRect(x, bottom, widths[index], height);
                stream.stroke();
                String value = header ? column.path("header").asText("")
                        : interpolate(column.path("value").asText(""), row);
                PDFont font = header ? bold : regular;
                value = fit(value, font, fontSize, widths[index] - padding * 2);
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(x + padding, bottom + (height - fontSize) / 2);
                stream.showText(value);
                stream.endText();
                x += widths[index];
            }
            y = bottom;
        }

        private float[] columnWidths(JsonNode columns) {
            float total = 0;
            for (JsonNode column : columns) total += column.path("weight").asDouble(1);
            float[] widths = new float[columns.size()];
            for (int index = 0; index < columns.size(); index++) {
                widths[index] = writableWidth() * (float) (columns.get(index).path("weight").asDouble(1) / total);
            }
            return widths;
        }

        private void space(float height) throws IOException {
            ensure(height);
            y -= height;
        }

        private void ensure(double height) throws IOException {
            if (!drawingPageHeader && y - height < margin) newPage();
        }

        private void newPage() throws IOException {
            closeStream();
            page = new PDPage(pageSize);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - margin;
            JsonNode header = template.path("pageHeader");
            if (header.isArray()) {
                drawingPageHeader = true;
                try { renderElements(header, root); }
                finally { drawingPageHeader = false; }
            }
        }

        private String interpolate(String value, JsonNode context) {
            Matcher matcher = EXPRESSION.matcher(value);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                JsonNode node = resolveNode(matcher.group(1).trim(), context);
                matcher.appendReplacement(result, Matcher.quoteReplacement(node.isValueNode() ? node.asText() : ""));
            }
            matcher.appendTail(result);
            return result.toString();
        }

        private JsonNode resolveNode(String path, JsonNode context) {
            if (path == null || path.isBlank() || ".".equals(path)) return context;
            JsonNode local = descend(context, path);
            return local.isMissingNode() ? descend(root, path) : local;
        }

        private JsonNode descend(JsonNode source, String path) {
            JsonNode current = source;
            for (String part : path.split("\\.")) {
                if (part.isBlank()) continue;
                current = current.path(part);
                if (current.isMissingNode()) return current;
            }
            return current;
        }

        private List<String> wrap(String text, PDFont font, float size, float available) throws IOException {
            if (text == null || text.isBlank()) return List.of("");
            List<String> result = new ArrayList<>();
            String remaining = text.trim().replaceAll("\\s+", " ");
            while (!remaining.isBlank()) {
                int cut = remaining.length();
                while (cut > 1 && width(remaining.substring(0, cut), font, size) > available) {
                    int space = remaining.lastIndexOf(' ', cut - 1);
                    cut = space > 0 ? space : cut - 1;
                }
                result.add(remaining.substring(0, cut).trim());
                remaining = remaining.substring(cut).trim();
            }
            return result;
        }

        private String fit(String value, PDFont font, float size, float available) throws IOException {
            String clean = Objects.toString(value, "").replaceAll("[\\r\\n]+", " ");
            if (width(clean, font, size) <= available) return clean;
            String suffix = "…";
            while (!clean.isEmpty() && width(clean + suffix, font, size) > available) {
                clean = clean.substring(0, clean.length() - 1);
            }
            return clean + suffix;
        }

        private float width(String value, PDFont font, float size) throws IOException {
            return font.getStringWidth(value) / 1000f * size;
        }

        private float writableWidth() {
            return page.getMediaBox().getWidth() - margin * 2;
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private static PDRectangle pageSize(JsonNode page) {
            PDRectangle size = switch (page.path("size").asText("A4").toUpperCase(Locale.ROOT)) {
                case "A3" -> PDRectangle.A3;
                case "LETTER" -> PDRectangle.LETTER;
                default -> PDRectangle.A4;
            };
            return "LANDSCAPE".equalsIgnoreCase(page.path("orientation").asText())
                    ? new PDRectangle(size.getHeight(), size.getWidth()) : size;
        }
    }
}
