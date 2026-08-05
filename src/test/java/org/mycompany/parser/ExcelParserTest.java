package org.mycompany.parser;

import org.mycompany.model.Record;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.junit.jupiter.api.Test;
import org.mycompany.parser.ExcelParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ExcelParser.
 * Builds XLSX files in-memory using POI.
 * No external fixtures needed.
 */
class ExcelParserTest {

    private final ExcelParser parser = new ExcelParser();

    @Test
    void parsesXlsxWithMixedCellTypesViaDataFormatter() throws IOException {
		// Header + 2 rows (String, Number, Boolean)
        byte[] xlsx = buildXlsx(
                new String[]{"shopname", "city", "zipcode", "hasonlineshop"},
                new Object[][]{
                        {"Test Shop A", "Berlin",  10115, true},
                        {"Test Shop B", "Hamburg", 20095, false}
                });

        try (Stream<Record> stream = parser.parse(new ByteArrayInputStream(xlsx))) {
            List<Record> records = stream.toList();

            assertEquals(2, records.size());

            Record first = records.get(0);
			// Excel row index starts at 1; header is row 1
            assertEquals(2,             first.rowNumber());
            assertEquals("Test Shop A", first.get("shopname").orElseThrow());
            assertEquals("Berlin",      first.get("city").orElseThrow());
			// DataFormatter removes ".0"
            assertEquals("10115",       first.get("zipcode").orElseThrow());
			// Boolean formatted as TRUE/FALSE
            assertEquals("TRUE",        first.get("hasonlineshop").orElseThrow());

            Record second = records.get(1);
            assertEquals(3,             second.rowNumber());
            assertEquals("Test Shop B", second.get("shopname").orElseThrow());
            assertEquals("20095",       second.get("zipcode").orElseThrow());
            assertEquals("FALSE",       second.get("hasonlineshop").orElseThrow());
        }
    }

    @Test
    void supportsRecognizesXlsxAndXlsCaseInsensitive() {
        assertTrue(parser.supports("foo.xlsx"));
        assertTrue(parser.supports("foo.xls"));
        assertTrue(parser.supports("FOO.XLSX"));
        assertFalse(parser.supports("foo.csv"));
        assertFalse(parser.supports("foo.txt"));
        assertFalse(parser.supports(null));
    }

    // --- Helpers ---------------------------------------------------------

    private static byte[] buildXlsx(String[] headers, Object[][] dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet();

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }

            for (int r = 0; r < dataRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < dataRows[r].length; c++) {
                    writeCell(row, c, dataRows[r][c]);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void writeCell(Row row, int column, Object value) {
        if (value instanceof String s) {
            row.createCell(column).setCellValue(s);
        } else if (value instanceof Number n) {
            row.createCell(column).setCellValue(n.doubleValue());
        } else if (value instanceof Boolean b) {
            row.createCell(column).setCellValue(b);
        }
    }
}
