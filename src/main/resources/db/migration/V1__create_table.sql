CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

CREATE TABLE roles
(
    code         VARCHAR(20) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE school_years
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    name       VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_school_year_dates
        CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX uq_current_school_year
    ON school_years (is_current)
    WHERE is_current = TRUE;

CREATE TABLE semesters
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    school_year_id UUID        NOT NULL
        REFERENCES school_years (id)
            ON DELETE CASCADE,
    semester_no    SMALLINT    NOT NULL
        CHECK (semester_no IN (1, 2)),
    name           VARCHAR(50) NOT NULL,
    start_date     DATE        NOT NULL,
    end_date       DATE        NOT NULL,
    is_current     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_semester
        UNIQUE (school_year_id, semester_no),
    CONSTRAINT ck_semester_dates
        CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX uq_current_semester_per_year
    ON semesters (school_year_id)
    WHERE is_current = TRUE;

CREATE TABLE classes
(
    id                    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    school_year_id        UUID        NOT NULL
        REFERENCES school_years (id)
            ON DELETE CASCADE,
    name                  VARCHAR(20) NOT NULL,
    grade_number          SMALLINT    NOT NULL
        CHECK (grade_number IN (10, 11, 12)),
    homeroom_teacher_name VARCHAR(150),
    room_name             VARCHAR(50),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_class_year
        UNIQUE (school_year_id, name)
);

CREATE TABLE users
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    class_id       UUID
        REFERENCES classes (id)
            ON DELETE SET NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    student_code   VARCHAR(20)  UNIQUE,
    full_name      VARCHAR(150) NOT NULL,
    date_of_birth  DATE,
    gender         VARCHAR(10)
        CHECK (gender IN (
                          'MALE',
                          'FEMALE',
                          'OTHER'
            )),
    avatar_url     TEXT,
    address        VARCHAR(255),
    guardian_name  VARCHAR(150),
    guardian_phone VARCHAR(20),
    role           VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'
        REFERENCES roles (code)
            ON UPDATE CASCADE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
                          'ACTIVE',
                          'LOCKED',
                          'INACTIVE'
            )),
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE subjects
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    code           VARCHAR(20)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    subject_group  VARCHAR(50)  NOT NULL DEFAULT 'OTHER',
    is_score_based BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE timetable_entries
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    class_id     UUID        NOT NULL
        REFERENCES classes (id)
            ON DELETE CASCADE,
    semester_id  UUID        NOT NULL
        REFERENCES semesters (id)
            ON DELETE CASCADE,
    subject_id   UUID        NOT NULL
        REFERENCES subjects (id)
            ON DELETE RESTRICT,
    day_of_week  SMALLINT    NOT NULL
        CHECK (day_of_week BETWEEN 1 AND 7),
    period_no    SMALLINT    NOT NULL
        CHECK (period_no BETWEEN 1 AND 15),
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    teacher_name VARCHAR(150),
    room_name    VARCHAR(50),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_timetable_time
        CHECK (end_time > start_time),
    CONSTRAINT uq_class_timetable
        UNIQUE (
                class_id,
                semester_id,
                day_of_week,
                period_no
            )
);

CREATE TABLE grades
(
    id              UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    subject_id      UUID          NOT NULL
        REFERENCES subjects (id)
            ON DELETE RESTRICT,
    semester_id     UUID          NOT NULL
        REFERENCES semesters (id)
            ON DELETE CASCADE,
    title           VARCHAR(150)  NOT NULL,
    component_code  VARCHAR(50),
    grade_type      VARCHAR(30)   NOT NULL
        CHECK (grade_type IN (
                              'REGULAR',
                              'MIDTERM',
                              'FINAL',
                              'ASSIGNMENT'
            )),
    score           NUMERIC(4, 2),
    weight          NUMERIC(4, 2) NOT NULL DEFAULT 1
        CHECK (weight > 0),
    max_score       NUMERIC(4, 2) NOT NULL DEFAULT 10
        CHECK (max_score > 0),
    comment         VARCHAR(255),
    assessment_date DATE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_grade_score
        CHECK (
            score IS NULL
                OR (
                score >= 0
                    AND score <= max_score
                )
            )
);

CREATE TABLE assignments
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    class_id       UUID         NOT NULL
        REFERENCES classes (id)
            ON DELETE CASCADE,
    subject_id     UUID         NOT NULL
        REFERENCES subjects (id)
            ON DELETE RESTRICT,
    semester_id    UUID         NOT NULL
        REFERENCES semesters (id)
            ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    teacher_name   VARCHAR(150),
    attachment_url TEXT,
    due_at         TIMESTAMPTZ,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED'
        CHECK (status IN (
                          'DRAFT',
                          'PUBLISHED',
                          'CLOSED'
            )),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE exams
(
    id               UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    class_id         UUID         NOT NULL
        REFERENCES classes (id)
            ON DELETE CASCADE,
    subject_id       UUID         NOT NULL
        REFERENCES subjects (id)
            ON DELETE RESTRICT,
    semester_id      UUID         NOT NULL
        REFERENCES semesters (id)
            ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    exam_type        VARCHAR(30)  NOT NULL
        CHECK (exam_type IN (
                             'MIDTERM',
                             'FINAL',
                             'OTHER'
            )),
    exam_date        DATE         NOT NULL,
    start_time       TIME         NOT NULL,
    duration_minutes INTEGER      NOT NULL
        CHECK (duration_minutes > 0),
    room_name        VARCHAR(50),
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE news_posts
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    title         VARCHAR(255) NOT NULL,
    summary       TEXT,
    content       TEXT         NOT NULL,
    thumbnail_url TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN (
                          'DRAFT',
                          'PUBLISHED',
                          'ARCHIVED'
            )),
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE notifications
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    body              TEXT         NOT NULL,
    notification_type VARCHAR(50)  NOT NULL,
    deep_link         TEXT,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_notification_read
        CHECK (
            (is_read = FALSE AND read_at IS NULL)
                OR
            (is_read = TRUE AND read_at IS NOT NULL)
            )
);

CREATE TABLE otp_challenges
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    phone         VARCHAR(20)  NOT NULL,
    otp_hash      VARCHAR(255) NOT NULL,
    purpose       VARCHAR(30)  NOT NULL DEFAULT 'FORGOT_PASSWORD'
        CHECK (purpose IN (
                           'FORGOT_PASSWORD',
                           'VERIFY_PHONE'
            )),
    expires_at    TIMESTAMPTZ  NOT NULL,
    attempt_count INTEGER      NOT NULL DEFAULT 0
        CHECK (attempt_count >= 0),
    max_attempts  INTEGER      NOT NULL DEFAULT 5
        CHECK (max_attempts > 0),
    verified_at   TIMESTAMPTZ,
    used_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_otp_expiration
        CHECK (expires_at > created_at)
);

CREATE TABLE request_types
(
    id                  UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    code                VARCHAR(50)  NOT NULL UNIQUE,
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    icon                VARCHAR(50),
    requires_date_range BOOLEAN      NOT NULL DEFAULT FALSE,
    requires_attachment BOOLEAN      NOT NULL DEFAULT FALSE,
    fields_json         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

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
    request_number VARCHAR(30)  NOT NULL UNIQUE,
    student_id     UUID         NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    type_id        UUID         NOT NULL
        REFERENCES request_types (id)
            ON DELETE RESTRICT,
    title          VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED'
        CHECK (status IN (
                          'SUBMITTED',
                          'PROCESSING',
                          'APPROVED',
                          'REJECTED',
                          'CANCELLED'
            )),
    form_data      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_student_requests_form_data_object
        CHECK (jsonb_typeof(form_data) = 'object')
);

CREATE TABLE request_attachments
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    request_id UUID        NOT NULL
        REFERENCES student_requests (id)
            ON DELETE CASCADE,
    file_id    UUID        NOT NULL
        REFERENCES uploaded_files (id)
            ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_request_attachment
        UNIQUE (request_id, file_id)
);

CREATE TABLE request_histories
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    request_id UUID        NOT NULL
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

CREATE TABLE clubs
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    public_id         VARCHAR(20)  NOT NULL UNIQUE,
    code              VARCHAR(50)  NOT NULL UNIQUE,
    name              VARCHAR(150) NOT NULL,
    description       TEXT,
    teacher_code      VARCHAR(20),
    teacher_name      VARCHAR(150),
    teacher_phone     VARCHAR(20),
    teacher_email     VARCHAR(150),
    location          VARCHAR(100),
    weekday_label     VARCHAR(20),
    start_time        TIME,
    end_time          TIME,
    member_count      INTEGER      NOT NULL DEFAULT 0 CHECK (member_count >= 0),
    max_members       INTEGER CHECK (max_members IS NULL OR max_members >= 0),
    registration_open BOOLEAN      NOT NULL DEFAULT TRUE,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_clubs_schedule_time
        CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time)
);

CREATE TABLE club_registrations
(
    id                  UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    student_id          UUID        NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    club_id             UUID        NOT NULL
        REFERENCES clubs (id)
            ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
                          'PENDING',
                          'JOINED',
                          'REJECTED',
                          'LEFT'
            )),
    reason              TEXT,
    cancellation_reason TEXT,
    registered_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at         TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_club_registration_student
        UNIQUE (student_id, club_id)
);

CREATE INDEX idx_users_class
    ON users (class_id);

CREATE INDEX idx_users_role
    ON users (role);

CREATE INDEX idx_timetable_class_semester_day
    ON timetable_entries (
                          class_id,
                          semester_id,
                          day_of_week,
                          period_no
        );

CREATE INDEX idx_grades_user_semester
    ON grades (
               user_id,
               semester_id,
               assessment_date DESC
        );

CREATE INDEX idx_assignments_class_due
    ON assignments (
                    class_id,
                    due_at
        );

CREATE INDEX idx_exams_class_date
    ON exams (
              class_id,
              exam_date
        );

CREATE INDEX idx_news_status_published
    ON news_posts (
                   status,
                   published_at DESC
        );

CREATE INDEX idx_notifications_user_read
    ON notifications (
                      user_id,
                      is_read,
                      created_at DESC
        );

CREATE INDEX idx_otp_phone_purpose_expires
    ON otp_challenges (
                       phone,
                       purpose,
                       expires_at DESC
        );

CREATE INDEX idx_student_requests_student_status_created
    ON student_requests (student_id, status, created_at DESC);

CREATE INDEX idx_student_requests_type_created
    ON student_requests (type_id, created_at DESC);

CREATE INDEX idx_request_attachments_request
    ON request_attachments (request_id);

CREATE INDEX idx_request_histories_request_created
    ON request_histories (request_id, created_at ASC);

CREATE INDEX idx_clubs_active_sort
    ON clubs (active, sort_order, name);

CREATE INDEX idx_club_registrations_student
    ON club_registrations (student_id, status, created_at DESC);

CREATE INDEX idx_club_registrations_club
    ON club_registrations (club_id, status);

COMMIT;
