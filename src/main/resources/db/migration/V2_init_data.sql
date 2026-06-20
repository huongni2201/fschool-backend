BEGIN;

INSERT INTO school_years (name, start_date, end_date, is_current)
VALUES ('2026-2027', DATE '2026-08-15', DATE '2027-05-31', TRUE)
ON CONFLICT (name) DO UPDATE
SET start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    is_current = EXCLUDED.is_current,
    updated_at = now();

INSERT INTO semesters (school_year_id, semester_no, name, start_date, end_date, is_current)
SELECT id, 1, 'Hoc ky 1', DATE '2026-08-15', DATE '2026-12-31', TRUE
FROM school_years
WHERE name = '2026-2027'
ON CONFLICT (school_year_id, semester_no) DO UPDATE
SET name = EXCLUDED.name,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    is_current = EXCLUDED.is_current,
    updated_at = now();

INSERT INTO semesters (school_year_id, semester_no, name, start_date, end_date, is_current)
SELECT id, 2, 'Hoc ky 2', DATE '2027-01-05', DATE '2027-05-31', FALSE
FROM school_years
WHERE name = '2026-2027'
ON CONFLICT (school_year_id, semester_no) DO UPDATE
SET name = EXCLUDED.name,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    is_current = EXCLUDED.is_current,
    updated_at = now();

INSERT INTO subjects (code, name, is_score_based)
VALUES ('MATH', 'Toan', TRUE),
       ('LITERATURE', 'Ngu van', TRUE),
       ('ENGLISH', 'Tieng Anh', TRUE),
       ('PHYSICS', 'Vat ly', TRUE),
       ('CHEMISTRY', 'Hoa hoc', TRUE),
       ('BIOLOGY', 'Sinh hoc', TRUE),
       ('INFORMATICS', 'Tin hoc', TRUE),
       ('HISTORY', 'Lich su', TRUE),
       ('GEOGRAPHY', 'Dia ly', TRUE),
       ('CIVICS', 'Giao duc kinh te va phap luat', TRUE),
       ('TECHNOLOGY', 'Cong nghe', TRUE),
       ('PE', 'Giao duc the chat', FALSE),
       ('DEFENSE', 'Giao duc quoc phong va an ninh', FALSE),
       ('LOCAL_EDU', 'Giao duc dia phuong', FALSE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_score_based = EXCLUDED.is_score_based,
    updated_at = now();

INSERT INTO classes (school_year_id, name, grade_number, room_name)
SELECT sy.id, class_data.name, class_data.grade_number, class_data.room_name
FROM school_years sy
CROSS JOIN (VALUES ('10A1', 10, 'A101'),
                   ('10A2', 10, 'A102'),
                   ('11A1', 11, 'A201'),
                   ('11A2', 11, 'A202'),
                   ('12A1', 12, 'A301'),
                   ('12A2', 12, 'A302')) AS class_data(name, grade_number, room_name)
WHERE sy.name = '2026-2027'
ON CONFLICT (school_year_id, name) DO UPDATE
SET grade_number = EXCLUDED.grade_number,
    room_name = EXCLUDED.room_name,
    updated_at = now();

INSERT INTO news_posts (title, summary, content, thumbnail_url, status, published_at)
SELECT 'Chao mung nam hoc 2026-2027',
       'Nha truong thong bao ke hoach khai giang nam hoc moi.',
       'Le khai giang nam hoc 2026-2027 du kien duoc to chuc vao ngay 05/09/2026.',
       NULL,
       'PUBLISHED',
       TIMESTAMPTZ '2026-08-20 08:00:00+07'
WHERE NOT EXISTS (
    SELECT 1 FROM news_posts WHERE title = 'Chao mung nam hoc 2026-2027'
);

INSERT INTO news_posts (title, summary, content, thumbnail_url, status, published_at)
SELECT 'Thong bao lich hoc chinh thuc',
       'Thoi khoa bieu hoc ky 1 se duoc ap dung tu ngay 17/08/2026.',
       'Hoc sinh kiem tra thoi khoa bieu tren ung dung truoc khi den truong.',
       NULL,
       'PUBLISHED',
       TIMESTAMPTZ '2026-08-14 09:00:00+07'
WHERE NOT EXISTS (
    SELECT 1 FROM news_posts WHERE title = 'Thong bao lich hoc chinh thuc'
);

COMMIT;
