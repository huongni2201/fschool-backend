package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record StudentRequestTypesResponse(
        List<RequestTypeResponse> requestTypes) {
}
