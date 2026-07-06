BEGIN;

CREATE TABLE request_types
(
    id                  UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    icon                VARCHAR(50),
    requires_date_range BOOLEAN     NOT NULL DEFAULT FALSE,
    requires_attachment BOOLEAN     NOT NULL DEFAULT FALSE,
    fields_json         JSONB       NOT NULL DEFAULT '[]'::jsonb,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order          INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_request_types_fields_json_array
        CHECK (jsonb_typeof(fields_json) = 'array')
);

CREATE TABLE uploaded_files
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    file_code   VARCHAR(50)  NOT NULL UNIQUE,
    file_name   VARCHAR(255) NOT NULL,
    url         TEXT         NOT NULL,
    mime_type   VARCHAR(100),
    size_bytes  BIGINT       NOT NULL CHECK (size_bytes >= 0),
    purpose     VARCHAR(50)  NOT NULL,
    uploaded_by UUID
                             REFERENCES users (id)
                                 ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE student_requests
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    request_number VARCHAR(30) NOT NULL UNIQUE,
    student_id     UUID        NOT NULL
                                REFERENCES users (id)
                                    ON DELETE CASCADE,
    type_id        UUID        NOT NULL
                                REFERENCES request_types (id)
                                    ON DELETE RESTRICT,
    title          VARCHAR(200) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
        CHECK (status IN (
                          'SUBMITTED',
                          'PROCESSING',
                          'APPROVED',
                          'REJECTED',
                          'CANCELLED'
            )),
    form_data      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_student_requests_form_data_object
        CHECK (jsonb_typeof(form_data) = 'object')
);

CREATE TABLE request_attachments
(
    id         UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    request_id UUID       NOT NULL
                              REFERENCES student_requests (id)
                                  ON DELETE CASCADE,
    file_id    UUID       NOT NULL
                              REFERENCES uploaded_files (id)
                                  ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_request_attachment
        UNIQUE (request_id, file_id)
);

CREATE TABLE request_histories
(
    id         UUID PRIMARY KEY    DEFAULT gen_random_uuid(),
    request_id UUID       NOT NULL
                              REFERENCES student_requests (id)
                                  ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL
        CHECK (status IN (
                          'SUBMITTED',
                          'PROCESSING',
                          'APPROVED',
                          'REJECTED',
                          'CANCELLED'
            )),
    note       VARCHAR(500),
    created_by UUID
                              REFERENCES users (id)
                                  ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_student_requests_student_status_created
    ON student_requests (student_id, status, created_at DESC);

CREATE INDEX idx_student_requests_type_created
    ON student_requests (type_id, created_at DESC);

CREATE INDEX idx_request_attachments_request
    ON request_attachments (request_id);

CREATE INDEX idx_request_histories_request_created
    ON request_histories (request_id, created_at ASC);

INSERT INTO request_types (
    code,
    name,
    description,
    icon,
    requires_date_range,
    requires_attachment,
    fields_json,
    sort_order
)
VALUES (
           'ABSENCE',
           'Đơn xin nghỉ học',
           'Gửi đơn nghỉ học có phép cho giáo viên chủ nhiệm.',
           'event_busy',
           TRUE,
           FALSE,
           '[
             {"key": "reason", "label": "Lý do nghỉ", "type": "textarea", "required": true},
             {"key": "fromDate", "label": "Từ ngày", "type": "date", "required": true},
             {"key": "toDate", "label": "Đến ngày", "type": "date", "required": true}
           ]'::jsonb,
           10
       ),
       (
           'STUDENT_CONFIRMATION',
           'Đơn xác nhận học sinh',
           'Yêu cầu xác nhận thông tin học sinh đang theo học.',
           'verified_user',
           FALSE,
           FALSE,
           '[
             {"key": "purpose", "label": "Mục đích xác nhận", "type": "textarea", "required": true}
           ]'::jsonb,
           20
       );

COMMIT;
