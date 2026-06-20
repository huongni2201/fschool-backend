package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import java.util.UUID;

public record LoginUserResponse(UUID id, String studentCode, String fullName, UserRole role, String className) {
}
