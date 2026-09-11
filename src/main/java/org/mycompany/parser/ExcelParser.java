package org.mycompany.parser;

import org.mycompany.model.Record;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Excel parser for .xlsx / .xls files.
 *
 * Responsibilities:
 * - Reads first sheet only
 * - Converts cells using DataFormatter (Excel-like output)
 * - Evaluates formulas safely via POI evaluator
 *
 * Security notes:
 * - ZIP bomb protection via ZipSecureFile
 * - Workbook is isolated per request (no shared state)
 * - Stream is closed via Stream.onClose()
 */

@Component
public class ExcelParser implements Parser {

	// prevents ZIP bomb / decompression attacks
	static {
		ZipSecureFile.setMinInflateRatio(0.005);
		ZipSecureFile.setMaxTextSize(10_000_000);
	}

	@Override
	public boolean supports(String filename) {
		if (filename == null) {
			return false;
		}
		String lower = filename.toLowerCase();
		return lower.endsWith(".xlsx") || lower.endsWith(".xls");
	}

	@Override
	public Stream<Record> parse(InputStream input) {
		Workbook workbook = openWorkbook(input);

		Sheet sheet = workbook.getSheetAt(0);
		Iterator<Row> rowIterator = sheet.rowIterator();

		if (!rowIterator.hasNext()) {
			closeQuietly(workbook);
			return Stream.empty();
		}

		DataFormatter formatter = new DataFormatter();
		FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

		List<String> headers = readHeaders(rowIterator.next(), formatter);

		Iterator<Row> safeIterator = rowIterator;

		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(safeIterator, Spliterator.ORDERED),
				false)
			.map(row -> toRecord(row, headers, formatter, evaluator))
			.onClose(() -> closeQuietly(workbook));
	}

	/**
	 * Opens workbook with Apache POI safe defaults.
	 *
	 * Security:
	 * - avoids custom XML parser configuration (POI handles safe defaults internally)
	 * - relies on ZipSecureFile hardening (static block)
	 */
	private static Workbook openWorkbook(InputStream input) {
		try {
			return WorkbookFactory.create(input);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read Excel file", e);
		}
	}

	private static List<String> readHeaders(Row headerRow, DataFormatter formatter) {
		List<String> headers = new ArrayList<>();
		int lastCol = headerRow.getLastCellNum();

		for (int i = 0; i < lastCol; i++) {
			Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

			if (cell == null) {
				headers.add(null);
				continue;
			}

			String text = formatter.formatCellValue(cell).trim();
			headers.add(text.isEmpty() ? null : text);
		}

		return headers;
	}

	private static Record toRecord(
		Row row,
		List<String> headers,
		DataFormatter formatter,
		FormulaEvaluator evaluator) {

		Map<String, String> fields = new LinkedHashMap<>();

		for (int i = 0; i < headers.size(); i++) {
			String header = headers.get(i);

			if (header == null) {
				continue;
			}

			Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

			String value = (cell == null)
				? ""
				: formatter.formatCellValue(cell, evaluator);

			// Store lowercase headers for case-insensitive Record.get() access.
			fields.put(header.toLowerCase(Locale.ROOT), value);
		}

		// Excel row index is 0-based → convert to human-readable index
		return new Record(row.getRowNum() + 1, fields);
	}

	private static void closeQuietly(Workbook workbook) {
		try {
			workbook.close();
		} catch (IOException ignored) {
			// intentionally ignored: cleanup best-effort
		}
	}
}
