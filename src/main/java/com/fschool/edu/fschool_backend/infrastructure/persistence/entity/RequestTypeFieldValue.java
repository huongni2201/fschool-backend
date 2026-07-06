package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RequestTypeFieldValue {

    private String key;
    private String label;
    private String type;
    private boolean required;
}
