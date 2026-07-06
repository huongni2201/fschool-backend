BEGIN;

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

CREATE INDEX idx_clubs_active_sort
    ON clubs (active, sort_order, name);

CREATE INDEX idx_club_registrations_student
    ON club_registrations (student_id, status, created_at DESC);

CREATE INDEX idx_club_registrations_club
    ON club_registrations (club_id, status);

INSERT INTO clubs (public_id,
                   code,
                   name,
                   description,
                   teacher_code,
                   teacher_name,
                   teacher_phone,
                   teacher_email,
                   location,
                   weekday_label,
                   start_time,
                   end_time,
                   member_count,
                   max_members,
                   registration_open,
                   sort_order)
VALUES ('CLB001',
        'MUSIC',
        U&'C\00E2u l\1EA1c b\1ED9 \00C2m nh\1EA1c',
        U&'Sinh ho\1EA1t, bi\1EC3u di\1EC5n v\00E0 ph\00E1t tri\1EC3n k\1EF9 n\0103ng \00E2m nh\1EA1c.',
        'T001',
        U&'Nguy\1EC5n V\0103n B',
        '0901234567',
        'teacher@example.com',
        U&'Ph\00F2ng \00C2m nh\1EA1c',
        U&'Th\1EE9 4',
        TIME '15:30',
        TIME '17:00',
        28,
        40,
        TRUE,
        10),
       ('CLB002',
        'FOOTBALL',
        U&'C\00E2u l\1EA1c b\1ED9 B\00F3ng \0111\00E1',
        U&'R\00E8n luy\1EC7n th\1EC3 l\1EF1c v\00E0 k\1EF9 n\0103ng b\00F3ng \0111\00E1.',
        'T002',
        U&'Tr\1EA7n V\0103n C',
        '0902345678',
        'football.teacher@example.com',
        U&'S\00E2n b\00F3ng',
        U&'Th\1EE9 6',
        TIME '16:00',
        TIME '17:30',
        35,
        45,
        TRUE,
        20);

WITH target_user AS (SELECT id
                     FROM users
                     WHERE student_code = 'HS0001'),
     target_club AS (SELECT id
                     FROM clubs
                     WHERE public_id = 'CLB001')
INSERT
INTO club_registrations (student_id, club_id, status, reason, registered_at, approved_at)
SELECT target_user.id,
       target_club.id,
       'JOINED',
       U&'Em mu\1ED1n tham gia \0111\1EC3 r\00E8n luy\1EC7n k\1EF9 n\0103ng v\00E0 giao l\01B0u v\1EDBi c\00E1c b\1EA1n.',
       TIMESTAMPTZ '2026-07-01 08:30:00+07',
       TIMESTAMPTZ '2026-07-02 09:00:00+07'
FROM target_user
    CROSS JOIN target_club
ON CONFLICT (student_id, club_id) DO NOTHING;

COMMIT;
