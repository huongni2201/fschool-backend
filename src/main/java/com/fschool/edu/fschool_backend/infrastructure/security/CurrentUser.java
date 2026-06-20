package com.fschool.edu.fschool_backend.infrastructure.security;

import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import java.util.UUID;

public record CurrentUser(UUID id, UserRole role) {
}
