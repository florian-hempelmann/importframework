package org.mycompany.parser;

import org.mycompany.model.Record;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Reads a structured file (Excel/CSV) and returns its rows as a stream of Records.
 * The first non-empty row is treated as the header. Its values are trimmed and used
 * as keys in Record.fields. These keys must match MappingConfig.sourceName entries.
 * The returned stream keeps native resources (Workbook/Reader) open.
 * Caller must close it (try-with-resources or explicit close) to avoid leaks.
 */
public interface Parser {

    Stream<Record> parse(InputStream input);

	/**
	 * Checks if this parser supports the given file by name/extension.
	 * Avoids central factory mapping for easier extensibility.
	 */
    boolean supports(String filename);
}
