package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "uploaded_files")
public class UploadedFileEntity extends AuditableEntity {

    @Column(name = "file_code", nullable = false, unique = true, length = 50)
    private String fileCode;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long size;

    @Column(nullable = false, length = 50)
    private String purpose;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
