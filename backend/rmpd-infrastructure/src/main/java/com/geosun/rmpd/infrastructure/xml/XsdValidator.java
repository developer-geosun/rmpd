package com.geosun.rmpd.infrastructure.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * Валідатор XML за XSD-схемами PUESC (якщо specs/ розпаковано).
 */
@Component
public class XsdValidator {

    private final Path xsdRoot;

    public XsdValidator(@Value("${rmpd.xsd-path:}") String xsdPath) {
        this.xsdRoot = xsdPath == null || xsdPath.isBlank() ? null : Path.of(xsdPath);
    }

    public boolean isXsdAvailable() {
        return xsdRoot != null && Files.isDirectory(xsdRoot);
    }

    public Path getXsdRoot() {
        return xsdRoot;
    }

    public List<String> validate(InputStream xmlStream) throws SAXException, IOException {
        if (!isXsdAvailable()) {
            throw new IllegalStateException("XSD path not configured or directory missing: " + xsdRoot);
        }

        Path mainXsd = findMainXsd(xsdRoot)
                .orElseThrow(() -> new IllegalStateException("No .xsd files found in " + xsdRoot));

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Schema schema = factory.newSchema(mainXsd.toFile());
        Validator validator = schema.newValidator();

        List<String> errors = new ArrayList<>();
        validator.setErrorHandler(new CollectingErrorHandler(errors));
        validator.validate(new StreamSource(xmlStream));

        return errors;
    }

    private static java.util.Optional<Path> findMainXsd(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> xsds = walk.filter(p -> p.toString().toLowerCase().endsWith(".xsd")).toList();
            return xsds.stream()
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("rmpd"))
                    .findFirst()
                    .or(() -> xsds.stream().findFirst());
        }
    }
}
