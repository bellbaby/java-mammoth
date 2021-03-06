package org.zwobble.mammoth.internal;

import org.zwobble.mammoth.internal.conversion.DocumentToHtml;
import org.zwobble.mammoth.internal.conversion.DocumentToHtmlOptions;
import org.zwobble.mammoth.internal.documents.DocumentElement;
import org.zwobble.mammoth.internal.documents.HasChildren;
import org.zwobble.mammoth.internal.documents.Paragraph;
import org.zwobble.mammoth.internal.documents.Text;
import org.zwobble.mammoth.internal.docx.DocxFile;
import org.zwobble.mammoth.internal.docx.InMemoryDocxFile;
import org.zwobble.mammoth.internal.docx.ZippedDocxFile;
import org.zwobble.mammoth.internal.html.Html;
import org.zwobble.mammoth.internal.results.InternalResult;
import org.zwobble.mammoth.internal.util.PassThroughException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.zwobble.mammoth.internal.docx.DocumentReader.readDocument;
import static org.zwobble.mammoth.internal.util.Casts.tryCast;
import static org.zwobble.mammoth.internal.util.Iterables.lazyMap;
import static org.zwobble.mammoth.internal.util.Lists.list;

public class InternalDocumentConverter {
    private final DocumentToHtmlOptions options;

    public InternalDocumentConverter(DocumentToHtmlOptions options) {
        this.options = options;
    }

    public InternalResult<String> convertToHtml(InputStream stream) throws IOException {
        return PassThroughException.unwrap(() ->
            withDocxFile(stream, zipFile ->
                convertToHtml(Optional.empty(), zipFile)));
    }

    public InternalResult<String> convertToHtml(File file) throws IOException {
        return PassThroughException.unwrap(() ->
            withDocxFile(file, zipFile ->
                convertToHtml(Optional.of(file.toPath()), zipFile)));
    }

    private InternalResult<String> convertToHtml(Optional<Path> path, DocxFile zipFile) {
        return readDocument(path, zipFile)
            .flatMap(nodes -> DocumentToHtml.convertToHtml(nodes, options))
            .map(Html::stripEmpty)
            .map(Html::collapse)
            .map(Html::write);
    }

    public InternalResult<String> extractRawText(InputStream stream) throws IOException {
        return PassThroughException.unwrap(() ->
            withDocxFile(stream, zipFile ->
                extractRawText(Optional.empty(), zipFile)));
    }

    public InternalResult<String> extractRawText(File file) throws IOException {
        return PassThroughException.unwrap(() ->
            withDocxFile(file, zipFile ->
                extractRawText(Optional.of(file.toPath()), zipFile)));
    }

    private InternalResult<String> extractRawText(Optional<Path> path, DocxFile zipFile) {
        return readDocument(path, zipFile)
            .map(InternalDocumentConverter::extractRawTextOfChildren);
    }

    private static <T> T withDocxFile(File file, Function<DocxFile, T> function) throws IOException {
        try (DocxFile zipFile = new ZippedDocxFile(file)) {
            return function.apply(zipFile);
        }
    }

    private static <T> T withDocxFile(InputStream stream, Function<DocxFile, T> function) throws IOException {
        try (DocxFile zipFile = InMemoryDocxFile.fromStream(stream)) {
            return function.apply(zipFile);
        }
    }

    private static String extractRawTextOfChildren(HasChildren parent) {
        return extractRawText(parent.getChildren());
    }

    private static String extractRawText(List<DocumentElement> nodes) {
        return String.join("", lazyMap(nodes, node -> extractRawText(node)));
    }

    private static String extractRawText(DocumentElement node) {
        return tryCast(Text.class, node)
            .map(Text::getValue)
            .orElseGet(() -> {
                List<DocumentElement> children = tryCast(HasChildren.class, node)
                    .map(HasChildren::getChildren)
                    .orElse(list());
                String suffix = tryCast(Paragraph.class, node).map(paragraph -> "\n\n").orElse("");
                return extractRawText(children) + suffix;
            });
    }
}
