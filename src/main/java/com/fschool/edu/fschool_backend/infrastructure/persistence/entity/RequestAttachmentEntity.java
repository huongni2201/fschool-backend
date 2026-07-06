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
@Table(name = "request_attachments")
public class RequestAttachmentEntity extends AuditableEntity {

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;
}
