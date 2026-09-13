package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.InputStream;
import org.apache.tika.detect.Detector;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.writefilter.StandardWriteFilterFactory;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.AutoDetectParserConfig;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.PasswordProvider;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.xml.sax.ContentHandler;

/** Creates a fresh, security-restricted Tika parser and parse context for each document. */
final class TikaParserFactory {

    private static final long MAXIMUM_COMPRESSION_RATIO = 100L;
    private static final long COMPRESSION_CHECK_THRESHOLD_BYTES = 100_000L;
    private static final int MAXIMUM_CONTAINER_DEPTH = 20;
    private static final int MAXIMUM_METADATA_BYTES = 100_000;
    private static final int MAXIMUM_METADATA_FIELD_BYTES = 16_384;
    private static final EmbeddedDocumentExtractor REJECT_EMBEDDED_DOCUMENTS =
            new EmbeddedDocumentExtractor() {
                @Override
                public boolean shouldParseEmbedded(Metadata metadata) {
                    return false;
                }

                @Override
                public void parseEmbedded(
                        InputStream stream,
                        ContentHandler handler,
                        Metadata metadata,
                        boolean outputHtml) {
                    // Deliberately do not parse attachments or embedded objects.
                }
            };

    private final Detector detector;
    private final long maximumPdfMainMemoryBytes;

    TikaParserFactory(Detector detector, long maximumPdfMainMemoryBytes) {
        this.detector = detector;
        this.maximumPdfMainMemoryBytes = maximumPdfMainMemoryBytes;
    }

    AutoDetectParser newParser() {
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        pdfConfig.setImageStrategy(PDFParserConfig.IMAGE_STRATEGY.NONE);
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setExtractActions(false);
        pdfConfig.setParseIncrementalUpdates(false);
        pdfConfig.setThrowOnEncryptedPayload(true);
        pdfConfig.setMaxMainMemoryBytes(maximumPdfMainMemoryBytes);

        PDFParser pdfParser = new PDFParser();
        pdfParser.setPDFParserConfig(pdfConfig);

        AutoDetectParser parser = new AutoDetectParser(detector, pdfParser, new OOXMLParser());
        parser.setAutoDetectParserConfig(newAutoDetectParserConfig());
        return parser;
    }

    ParseContext newParseContext() {
        OfficeParserConfig officeConfig = new OfficeParserConfig();
        officeConfig.setExtractMacros(false);
        officeConfig.setIncludeDeletedContent(false);
        officeConfig.setIncludeMoveFromContent(false);
        officeConfig.setUseSAXDocxExtractor(true);

        ParseContext context = new ParseContext();
        context.set(OfficeParserConfig.class, officeConfig);
        context.set(EmbeddedDocumentExtractor.class, REJECT_EMBEDDED_DOCUMENTS);
        context.set(PasswordProvider.class, metadata -> null);
        return context;
    }

    private static AutoDetectParserConfig newAutoDetectParserConfig() {
        AutoDetectParserConfig parserConfig = new AutoDetectParserConfig();
        parserConfig.setSpoolToDisk(0L);
        parserConfig.setOutputThreshold(COMPRESSION_CHECK_THRESHOLD_BYTES);
        parserConfig.setMaximumCompressionRatio(MAXIMUM_COMPRESSION_RATIO);
        parserConfig.setMaximumDepth(MAXIMUM_CONTAINER_DEPTH);
        parserConfig.setMaximumPackageEntryDepth(MAXIMUM_CONTAINER_DEPTH);

        StandardWriteFilterFactory metadataFilter = new StandardWriteFilterFactory();
        metadataFilter.setMaxTotalEstimatedBytes(MAXIMUM_METADATA_BYTES);
        metadataFilter.setMaxFieldSize(MAXIMUM_METADATA_FIELD_BYTES);
        parserConfig.setMetadataWriteFilterFactory(metadataFilter);
        return parserConfig;
    }
}
