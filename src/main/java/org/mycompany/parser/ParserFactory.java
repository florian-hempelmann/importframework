package org.mycompany.parser;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Selects the correct parser based on file name.
 * Parsers are injected via constructor (Spring can auto-wire all beans).
 * New parsers are picked up automatically if registered as beans and
 * if supports(...) returns true.
 */

@Component
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
                        "No parser found for file '" + filename
                                + "'. Supported extensions: .xlsx, .xls"));
    }
}
