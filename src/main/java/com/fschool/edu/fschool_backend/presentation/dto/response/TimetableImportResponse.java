package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;
import java.util.UUID;

public record TimetableImportResponse(
        UUID importId,
        String fileName,
        int totalRows,
        int successRows,
        int failedRows,
        String status,
        List<ImportError> errors) {

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public record ImportError(
            int rowNumber,
            String column,
            String message) {
    }
}
