/**
 * Explicit JPMS descriptor for utilities-pack.
 *
 * This fixes a hard blocker for the whole Megalodonte ecosystem: before this existed, the
 * JitPack artifact was consumed as {@code pack-utilities-v1.0.0.jar}, which has neither a
 * {@code module-info.class} nor an {@code Automatic-Module-Name}. javac derived the module
 * name {@code pack.utilities.v1.0.0} and rejected it:
 *
 * <pre>
 *   jar --describe-module --file=pack-utilities-v1.0.0.jar
 *   Unable to derive module descriptor
 *   pack.utilities.v1.0.0: Invalid module name: '0' is not a Java identifier
 * </pre>
 *
 * The manifest {@code Automatic-Module-Name} fallback is kept in build.gradle.kts as a
 * belt-and-braces measure for consumers still resolving an older published jar.
 *
 * PDF generation uses Jackson for data/templates and Apache PDFBox for rendering.
 */
module pack.utilities {
    requires com.fasterxml.jackson.databind;
    requires org.apache.pdfbox;

    exports pack.utilities;
    exports pack.utilities.pdf;
}
