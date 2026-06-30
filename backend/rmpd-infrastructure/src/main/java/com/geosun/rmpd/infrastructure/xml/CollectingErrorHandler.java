package com.geosun.rmpd.infrastructure.xml;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

class CollectingErrorHandler implements ErrorHandler {

    private final List<String> errors;

    CollectingErrorHandler(List<String> errors) {
        this.errors = errors;
    }

    @Override
    public void warning(SAXParseException exception) {
        errors.add("warning: " + format(exception));
    }

    @Override
    public void error(SAXParseException exception) {
        errors.add("error: " + format(exception));
    }

    @Override
    public void fatalError(SAXParseException exception) {
        errors.add("fatal: " + format(exception));
    }

    private static String format(SAXParseException e) {
        return String.format("line %d, col %d: %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
    }
}
