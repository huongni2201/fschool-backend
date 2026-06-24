CREATE
EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- =====================================================================
-- 1. NĂM HỌC
-- =====================================================================

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
    ON school_years (is_current) WHERE is_current = TRUE;

-- =====================================================================
-- 2. HỌC KỲ
-- =====================================================================

CREATE TABLE semesters
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    school_year_id  UUID NOT NULL
        REFERENCES school_years(id)
        ON DELETE CASCADE,

    semester_no     SMALLINT NOT NULL
        CHECK (semester_no IN (1, 2)),

    name            VARCHAR(50) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    is_current      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_semester
        UNIQUE (school_year_id, semester_no),

    CONSTRAINT ck_semester_dates
        CHECK (end_date > start_date)

);

CREATE UNIQUE INDEX uq_current_semester_per_year
    ON semesters (school_year_id) WHERE is_current = TRUE;

-- =====================================================================
-- 3. LỚP HỌC
-- =====================================================================

CREATE TABLE classes
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    school_year_id          UUID NOT NULL
        REFERENCES school_years(id)
        ON DELETE CASCADE,

    name                    VARCHAR(20) NOT NULL,

    grade_number            SMALLINT NOT NULL
        CHECK (grade_number IN (10, 11, 12)),

    homeroom_teacher_name   VARCHAR(150),
    room_name               VARCHAR(50),

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_class_year
        UNIQUE (school_year_id, name)

);

-- =====================================================================
-- 4. NGƯỜI DÙNG / HỌC SINH
-- =====================================================================

CREATE TABLE users
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    class_id        UUID
        REFERENCES classes(id)
        ON DELETE SET NULL,

    phone           VARCHAR(20) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    student_code    VARCHAR(20) NOT NULL UNIQUE,
    full_name       VARCHAR(150) NOT NULL,
    date_of_birth   DATE,

    gender          VARCHAR(10)
        CHECK (gender IN (
            'MALE',
            'FEMALE',
            'OTHER'
        )),

    avatar_url      TEXT,
    address         VARCHAR(255),
    guardian_name   VARCHAR(150),
    guardian_phone  VARCHAR(20),

    role            VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
        CHECK (role IN (
            'STUDENT',
            'ADMIN'
        )),

    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN (
            'ACTIVE',
            'LOCKED',
            'INACTIVE'
        )),

    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()

);

-- =====================================================================
-- 5. MÔN HỌC
-- =====================================================================

CREATE TABLE subjects
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    code           VARCHAR(20)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    is_score_based BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- =====================================================================
-- 6. THỜI KHÓA BIỂU
-- =====================================================================

CREATE TABLE timetable_entries
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    class_id        UUID NOT NULL
        REFERENCES classes(id)
        ON DELETE CASCADE,

    semester_id     UUID NOT NULL
        REFERENCES semesters(id)
        ON DELETE CASCADE,

    subject_id      UUID NOT NULL
        REFERENCES subjects(id)
        ON DELETE RESTRICT,

    day_of_week     SMALLINT NOT NULL
        CHECK (day_of_week BETWEEN 1 AND 7),

    period_no       SMALLINT NOT NULL
        CHECK (period_no BETWEEN 1 AND 15),

    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    teacher_name    VARCHAR(150),
    room_name       VARCHAR(50),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

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

-- =====================================================================
-- 7. ĐIỂM SỐ
-- =====================================================================

CREATE TABLE grades
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id         UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    subject_id      UUID NOT NULL
        REFERENCES subjects(id)
        ON DELETE RESTRICT,

    semester_id     UUID NOT NULL
        REFERENCES semesters(id)
        ON DELETE CASCADE,

    title           VARCHAR(150) NOT NULL,

    grade_type      VARCHAR(30) NOT NULL
        CHECK (grade_type IN (
            'REGULAR',
            'MIDTERM',
            'FINAL',
            'ASSIGNMENT'
        )),

    score           NUMERIC(4,2),

    weight          NUMERIC(4,2) NOT NULL DEFAULT 1
        CHECK (weight > 0),

    max_score       NUMERIC(4,2) NOT NULL DEFAULT 10
        CHECK (max_score > 0),

    comment         VARCHAR(255),
    assessment_date DATE,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_grade_score
        CHECK (
            score IS NULL
            OR (
                score >= 0
                AND score <= max_score
            )
        )

);

-- =====================================================================
-- 8. BÀI TẬP
-- =====================================================================

CREATE TABLE assignments
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    class_id        UUID NOT NULL
        REFERENCES classes(id)
        ON DELETE CASCADE,

    subject_id      UUID NOT NULL
        REFERENCES subjects(id)
        ON DELETE RESTRICT,

    semester_id     UUID NOT NULL
        REFERENCES semesters(id)
        ON DELETE CASCADE,

    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    teacher_name    VARCHAR(150),
    attachment_url  TEXT,
    due_at          TIMESTAMPTZ,

    status          VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
        CHECK (status IN (
            'DRAFT',
            'PUBLISHED',
            'CLOSED'
        )),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()

);

-- =====================================================================
-- 9. LỊCH THI
-- =====================================================================

CREATE TABLE exams
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    class_id            UUID NOT NULL
        REFERENCES classes(id)
        ON DELETE CASCADE,

    subject_id          UUID NOT NULL
        REFERENCES subjects(id)
        ON DELETE RESTRICT,

    semester_id         UUID NOT NULL
        REFERENCES semesters(id)
        ON DELETE CASCADE,

    title               VARCHAR(200) NOT NULL,

    exam_type           VARCHAR(30) NOT NULL
        CHECK (exam_type IN (
            'MIDTERM',
            'FINAL',
            'OTHER'
        )),

    exam_date           DATE NOT NULL,
    start_time          TIME NOT NULL,

    duration_minutes    INTEGER NOT NULL
        CHECK (duration_minutes > 0),

    room_name           VARCHAR(50),
    note                VARCHAR(255),

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()

);

-- =====================================================================
-- 10. TIN TỨC
-- =====================================================================

CREATE TABLE news_posts
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(255) NOT NULL,
    summary       TEXT,
    content       TEXT         NOT NULL,
    thumbnail_url TEXT,

    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN (
            'DRAFT',
            'PUBLISHED',
            'ARCHIVED'
        )),

    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()

);

-- =====================================================================
-- 11. THÔNG BÁO
-- =====================================================================

CREATE TABLE notifications
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id             UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    title               VARCHAR(255) NOT NULL,
    body                TEXT NOT NULL,
    notification_type   VARCHAR(50) NOT NULL,
    deep_link           TEXT,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_notification_read
        CHECK (
            (is_read = FALSE AND read_at IS NULL)
            OR
            (is_read = TRUE AND read_at IS NOT NULL)
        )

);

-- =====================================================================
-- 12. OTP QUÊN MẬT KHẨU
-- =====================================================================

CREATE TABLE otp_challenges
(
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone    VARCHAR(20)  NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,

    purpose         VARCHAR(30) NOT NULL DEFAULT 'FORGOT_PASSWORD'
        CHECK (purpose IN (
            'FORGOT_PASSWORD',
            'VERIFY_PHONE'
        )),

    expires_at      TIMESTAMPTZ NOT NULL,

    attempt_count   INTEGER NOT NULL DEFAULT 0
        CHECK (attempt_count >= 0),

    max_attempts    INTEGER NOT NULL DEFAULT 5
        CHECK (max_attempts > 0),

    verified_at     TIMESTAMPTZ,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_otp_expiration
        CHECK (expires_at > created_at)

);

-- =====================================================================
-- INDEXES
-- =====================================================================

CREATE INDEX idx_users_class
    ON users (class_id);

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

COMMIT;
