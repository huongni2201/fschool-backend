package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.enums.ContentStatus;
import com.fschool.edu.fschool_backend.domain.exception.BusinessRuleViolationException;
import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import java.time.Instant;
import lombok.Getter;

@Getter
public class NewsPostAggregate extends AuditableAggregateRoot {

    private String title;
    private String summary;
    private String content;
    private String thumbnailUrl;
    private ContentStatus status;
    private Instant publishedAt;

    public NewsPostAggregate(
            EntityId id,
            String title,
            String summary,
            String content,
            String thumbnailUrl,
            ContentStatus status,
            Instant publishedAt,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        rename(title);
        changeSummary(summary);
        changeContent(content);
        this.thumbnailUrl = normalizeOptional(thumbnailUrl, 2048, "Thumbnail URL");
        this.status = status == null ? ContentStatus.DRAFT : status;
        this.publishedAt = publishedAt;
    }

    public void rename(String title) {
        title = title == null ? "" : title.trim();
        if (title.isBlank() || title.length() > 255) {
            throw new DomainValidationException("News title is required and must not exceed 255 characters");
        }
        this.title = title;
        markUpdated();
    }

    public void changeSummary(String summary) {
        this.summary = normalizeOptional(summary, 10_000, "Summary");
        markUpdated();
    }

    public void changeContent(String content) {
        content = content == null ? "" : content.trim();
        if (content.isBlank()) {
            throw new DomainValidationException("News content is required");
        }
        this.content = content;
        markUpdated();
    }

    public void publish(Instant publishedAt) {
        if (status == ContentStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("Archived news cannot be published again");
        }
        status = ContentStatus.PUBLISHED;
        this.publishedAt = publishedAt == null ? Instant.now() : publishedAt;
        markUpdated();
    }

    public void archive() {
        status = ContentStatus.ARCHIVED;
        markUpdated();
    }

    public void moveToDraft() {
        if (status == ContentStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("Archived news cannot be moved to draft");
        }
        status = ContentStatus.DRAFT;
        publishedAt = null;
        markUpdated();
    }

    private String normalizeOptional(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();
        if (value.length() > maxLength) {
            throw new DomainValidationException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
