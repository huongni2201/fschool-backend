package com.fschool.edu.fschool_backend.presentation.exception;

import java.util.List;
import java.util.Map;

public class RequestValidationException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public RequestValidationException(Map<String, List<String>> errors) {
        super("Request is invalid");
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
