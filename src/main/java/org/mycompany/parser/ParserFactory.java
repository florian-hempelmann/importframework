package org.mycompany.parser;

import java.util.List;

/**
 * Selects the correct parser based on file name.
 * Parsers are injected via constructor (Spring can auto-wire all beans).
 * New parsers are picked up automatically if registered as beans and
 * if supports(...) returns true.
 */
public class ParserFactory {

    private final List<Parser> parsers;

    public ParserFactory(List<Parser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public Parser forFile(String filename) {
        return parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kein Parser fuer Datei '" + filename
                                + "'. Unterstuetzt sind: .xlsx, .xls"));
    }
}
