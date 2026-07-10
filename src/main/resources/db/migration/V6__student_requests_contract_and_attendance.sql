BEGIN;

UPDATE request_types
SET is_active = FALSE,
    updated_at = now()
WHERE code IN ('ABSENCE', 'STUDENT_CONFIRMATION');

INSERT INTO request_types (
    code,
    name,
    description,
    icon,
    requires_date_range,
    requires_attachment,
    fields_json,
    is_active,
    sort_order
)
VALUES (
           'absence',
           U&'\0110\01A1n xin ngh\1EC9 h\1ECDc',
           U&'G\1EEDi \0111\01A1n ngh\1EC9 h\1ECDc c\00F3 ph\00E9p cho gi\00E1o vi\00EAn ch\1EE7 nhi\1EC7m.',
           'absence',
           TRUE,
           FALSE,
           U&'[{"key":"reason","label":"L\00FD do ngh\1EC9","type":"textarea","required":true}]'::jsonb,
           TRUE,
           10
       ),
       (
           'confirmation',
           U&'\0110\01A1n x\00E1c nh\1EADn h\1ECDc sinh',
           U&'Y\00EAu c\1EA7u x\00E1c nh\1EADn th\00F4ng tin h\1ECDc sinh \0111ang theo h\1ECDc.',
           'verified',
           FALSE,
           FALSE,
           U&'[{"key":"purpose","label":"M\1EE5c \0111\00EDch x\00E1c nh\1EADn","type":"text","required":true}]'::jsonb,
           TRUE,
           20
       )
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    icon = EXCLUDED.icon,
    requires_date_range = EXCLUDED.requires_date_range,
    requires_attachment = EXCLUDED.requires_attachment,
    fields_json = EXCLUDED.fields_json,
    is_active = TRUE,
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

CREATE TABLE student_attendance_records
(
    id                 UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    student_id         UUID        NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    class_id           UUID
        REFERENCES classes (id)
            ON DELETE SET NULL,
    subject_id         UUID
        REFERENCES subjects (id)
            ON DELETE SET NULL,
    timetable_entry_id UUID
        REFERENCES timetable_entries (id)
            ON DELETE SET NULL,
    attendance_date    DATE        NOT NULL,
    period_no          SMALLINT
        CHECK (period_no IS NULL OR period_no BETWEEN 1 AND 15),
    start_time         TIME,
    end_time           TIME,
    teacher_name       VARCHAR(150),
    status             VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN'
        CHECK (status IN (
                          'PRESENT',
                          'ABSENT',
                          'LATE',
                          'EXCUSED',
                          'UNKNOWN'
            )),
    note               VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_student_attendance_time
        CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time),
    CONSTRAINT uq_student_attendance_session
        UNIQUE (student_id, attendance_date, period_no)
);

CREATE INDEX idx_student_attendance_student_date
    ON student_attendance_records (student_id, attendance_date, period_no);

CREATE INDEX idx_student_attendance_subject
    ON student_attendance_records (subject_id);

COMMIT;
