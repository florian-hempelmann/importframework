package org.mycompany.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldImportValidExcelFile() throws Exception {
        MockMultipartFile file = validExcelFile();

        mockMvc.perform(
                        multipart("/api/imports/wheretobuy-sqlite")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("wheretobuy-sqlite"))
                .andExpect(jsonPath("$.strategy").value("replaceFolder"))
                .andExpect(jsonPath("$.executedBy").value("unknown"))
                .andExpect(jsonPath("$.totals.read").isNumber())
                .andExpect(jsonPath("$.totals.succeeded").isNumber())
                .andExpect(jsonPath("$.totals.failed").value(0))
                .andExpect(jsonPath("$.failures").isArray());
    }

    @Test
    void shouldReportValidationErrorsForInvalidExcelFile() throws Exception {
        MockMultipartFile file = invalidExcelFile();

        mockMvc.perform(
                        multipart("/api/imports/wheretobuy-sqlite")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("wheretobuy-sqlite"))
                .andExpect(jsonPath("$.strategy").value("replaceFolder"))
                .andExpect(jsonPath("$.totals.failed").value(greaterThan(0)))
                .andExpect(jsonPath("$.failures").isArray())
                .andExpect(jsonPath("$.failures", hasSize(greaterThan(0))));
    }

    @Test
    void shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]
        );

        mockMvc.perform(
                        multipart("/api/imports/wheretobuy-sqlite")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));
    }

    @Test
    void shouldRejectUnknownImportType() throws Exception {
        MockMultipartFile file = validExcelFile();

        mockMvc.perform(
                        multipart("/api/imports/unknown-type")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("No mapping configuration for import type 'unknown-type' found "
                                + "(expected in /mappings/unknown-type.yaml)"));
    }

    @Test
    void shouldRejectUnsupportedFileExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "shops.csv",
                "text/csv",
                "shopname\nTest Shop".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/imports/wheretobuy-sqlite")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("No parser found for file 'shops.csv'. Supported extensions: .xlsx, .xls"));
    }

    private MockMultipartFile validExcelFile() throws IOException {
        try (InputStream inputStream =
                     getClass().getResourceAsStream("/wheretobuy/valid.xlsx")) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Test file not found: /wheretobuy/valid.xlsx"
                );
            }

            return new MockMultipartFile(
                    "file",
                    "valid.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    inputStream
            );
        }
    }

    private MockMultipartFile invalidExcelFile() throws IOException {
        try (InputStream inputStream =
                     getClass().getResourceAsStream("/wheretobuy/invalid.xlsx")) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Test file not found: /wheretobuy/invalid.xlsx"
                );
            }

            return new MockMultipartFile(
                    "file",
                    "invalid.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    inputStream
            );
        }
    }
}
