BEGIN;

INSERT INTO school_years (name, start_date, end_date, is_current)
VALUES ('2026-2027', DATE '2026-08-15', DATE '2027-05-31', TRUE);

INSERT INTO semesters (school_year_id, semester_no, name, start_date, end_date, is_current)
SELECT id, 1, 'Hoc ky 1', DATE '2026-08-15', DATE '2026-12-31', TRUE
FROM school_years
WHERE name = '2026-2027';

INSERT INTO semesters (school_year_id, semester_no, name, start_date, end_date, is_current)
SELECT id, 2, 'Hoc ky 2', DATE '2027-01-05', DATE '2027-05-31', FALSE
FROM school_years
WHERE name = '2026-2027';

INSERT INTO subjects (code, name, subject_group, is_score_based)
VALUES ('MATH', 'Toan', 'NATURAL', TRUE),
       ('LITERATURE', 'Ngu van', 'SOCIAL', TRUE),
       ('ENGLISH', 'Tieng Anh', 'FOREIGN_LANGUAGE', TRUE),
       ('PHYSICS', 'Vat ly', 'NATURAL', TRUE),
       ('CHEMISTRY', 'Hoa hoc', 'NATURAL', TRUE),
       ('BIOLOGY', 'Sinh hoc', 'NATURAL', TRUE),
       ('INFORMATICS', 'Tin hoc', 'NATURAL', TRUE),
       ('HISTORY', 'Lich su', 'SOCIAL', TRUE),
       ('GEOGRAPHY', 'Dia ly', 'SOCIAL', TRUE),
       ('CIVICS', 'Giao duc kinh te va phap luat', 'SOCIAL', TRUE),
       ('TECHNOLOGY', 'Cong nghe', 'NATURAL', TRUE),
       ('PE', 'Giao duc the chat', 'SKILL', FALSE),
       ('DEFENSE', 'Giao duc quoc phong va an ninh', 'SKILL', FALSE),
       ('LOCAL_EDU', 'Giao duc dia phuong', 'SOCIAL', FALSE);

INSERT INTO classes (school_year_id, name, grade_number, room_name)
SELECT sy.id, class_data.name, class_data.grade_number, class_data.room_name
FROM school_years sy
CROSS JOIN (VALUES ('10A1', 10, 'A101'),
                   ('10A2', 10, 'A102'),
                   ('11A1', 11, 'A201'),
                   ('11A2', 11, 'A202'),
                   ('12A1', 12, 'A301'),
                   ('12A2', 12, 'A302')) AS class_data(name, grade_number, room_name)
WHERE sy.name = '2026-2027';

INSERT INTO news_posts (title, summary, content, thumbnail_url, status, published_at)
VALUES ('Chao mung nam hoc 2026-2027',
        'Nha truong thong bao ke hoach khai giang nam hoc moi.',
        'Le khai giang nam hoc 2026-2027 du kien duoc to chuc vao ngay 05/09/2026.',
        NULL,
        'PUBLISHED',
        TIMESTAMPTZ '2026-08-20 08:00:00+07'),
       ('Thong bao lich hoc chinh thuc',
        'Thoi khoa bieu hoc ky 1 se duoc ap dung tu ngay 17/08/2026.',
        'Hoc sinh kiem tra thoi khoa bieu tren ung dung truoc khi den truong.',
        NULL,
        'PUBLISHED',
        TIMESTAMPTZ '2026-08-14 09:00:00+07');

COMMIT;
