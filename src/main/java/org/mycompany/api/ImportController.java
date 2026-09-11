package org.mycompany.api;

import org.mycompany.model.ImportReport;
import org.mycompany.service.ImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(
            value = "/{type}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ImportReport importFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Uploaded file has no filename");
        }

        try {
            return importService.runImport(
                    type,
                    filename,
                    file.getInputStream(),
                    "unknown"
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read uploaded file '" + filename + "'",
                    e
            );
        }
    }
}