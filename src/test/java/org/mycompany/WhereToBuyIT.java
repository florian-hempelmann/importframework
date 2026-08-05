package org.mycompany;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mycompany.model.ImportReport;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WhereToBuyIT {

	private final ObjectMapper mapper = new ObjectMapper()
		.registerModule(new JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@Test
	void shouldImportValidExcel() throws IOException, InterruptedException {

		Result result = executeCurl("valid.xlsx");

		assertEquals(200, result.status());

		ImportReport report = parseReport(result.bodyPath());

		assertEquals(0, report.totals().failed());
		assertTrue(report.totals().succeeded() > 0);
		assertFalse(report.hasFailures());
	}

	@Test
	void shouldImportInvalidExcelAndReturnValidationErrors() throws IOException, InterruptedException {

		Result result = executeCurl("invalid.xlsx");

		// import succeeded as request, but contains validation failures
		assertEquals(200, result.status());

		ImportReport report = parseReport(result.bodyPath());

		// key assertion: business validation, not HTTP status
		assertTrue(report.totals().failed() > 0);
		assertTrue(report.hasFailures());

		assertEquals(3, report.failures().size());

		assertTrue(report.failures().stream()
				.anyMatch(f -> f.reason().contains("required")));
	}

	private ImportReport parseReport(Path file) throws IOException {
		return mapper.readValue(Files.readString(file), ImportReport.class);
	}

	private Result executeCurl(String file) throws IOException, InterruptedException {

		Path outFile = Files.createTempFile("import-response", ".json");

		Process process = new ProcessBuilder(
			"curl",
			"-s",
			"-o", outFile.toString(),   // store full JSON response
			"-w", "%{http_code}",
			"-u", "admin:admin",
			"-F", "file=@src/test/resources/wheretobuy/" + file,
			"http://localhost:8080/cms/ws/imports/wheretobuy"
		).start();

		BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream())
		);

		String status = reader.readLine();

		process.waitFor();

		return new Result(Integer.parseInt(status), outFile);
	}

	record Result(int status, Path bodyPath) {}
}
