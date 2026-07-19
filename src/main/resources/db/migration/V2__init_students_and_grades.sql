BEGIN;

INSERT INTO roles (code, display_name, description)
VALUES ('STUDENT', 'Student', 'Student account'),
       ('PARENT', 'Parent', 'Parent or guardian account'),
       ('TEACHER', 'Teacher', 'Teacher account'),
       ('ADMIN', 'Admin', 'Administrator account');

INSERT INTO school_years (name, start_date, end_date, is_current)
VALUES ('2026-2027', DATE '2026-08-15', DATE '2027-05-31', TRUE);

INSERT INTO semesters (school_year_id, semester_no, name, start_date, end_date, is_current)
SELECT sy.id, data.semester_no, data.name, data.start_date, data.end_date, data.is_current
FROM school_years sy
CROSS JOIN (VALUES
    (1, 'Hoc ky 1', DATE '2026-08-15', DATE '2026-12-31', TRUE),
    (2, 'Hoc ky 2', DATE '2027-01-05', DATE '2027-05-31', FALSE)
) AS data(semester_no, name, start_date, end_date, is_current)
WHERE sy.name = '2026-2027';

INSERT INTO subjects (code, name, subject_group, is_score_based, grade_levels, lessons_per_week, status)
VALUES ('MATH', 'Toan', 'NATURAL', TRUE, '10,11,12', 4, 'ACTIVE'),
       ('LITERATURE', 'Ngu van', 'SOCIAL', TRUE, '10,11,12', 4, 'ACTIVE'),
       ('ENGLISH', 'Tieng Anh', 'FOREIGN_LANGUAGE', TRUE, '10,11,12', 3, 'ACTIVE'),
       ('PHYSICS', 'Vat ly', 'NATURAL', TRUE, '10,11,12', 2, 'ACTIVE'),
       ('CHEMISTRY', 'Hoa hoc', 'NATURAL', TRUE, '10,11,12', 2, 'ACTIVE'),
       ('BIOLOGY', 'Sinh hoc', 'NATURAL', TRUE, '10,11,12', 2, 'ACTIVE'),
       ('INFORMATICS', 'Tin hoc', 'NATURAL', TRUE, '10,11,12', 1, 'ACTIVE'),
       ('HISTORY', 'Lich su', 'SOCIAL', TRUE, '10,11,12', 2, 'ACTIVE'),
       ('GEOGRAPHY', 'Dia ly', 'SOCIAL', TRUE, '10,11,12', 2, 'ACTIVE'),
       ('CIVICS', 'Giao duc kinh te va phap luat', 'SOCIAL', TRUE, '10,11,12', 1, 'ACTIVE'),
       ('TECHNOLOGY', 'Cong nghe', 'NATURAL', TRUE, '10,11,12', 1, 'ACTIVE'),
       ('PE', 'Giao duc the chat', 'SKILL', FALSE, '10,11,12', 2, 'ACTIVE'),
       ('DEFENSE', 'Giao duc quoc phong va an ninh', 'SKILL', FALSE, '10,11,12', 1, 'ACTIVE'),
       ('LOCAL_EDU', 'Giao duc dia phuong', 'SOCIAL', FALSE, '10,11,12', 1, 'ACTIVE');

INSERT INTO classes (school_year_id, name, grade_number, room_name, status)
SELECT sy.id,
       class_data.name,
       class_data.grade_number,
       class_data.room_name,
       'ACTIVE'
FROM school_years sy
CROSS JOIN (VALUES
    ('10A1', 10, 'A101'),
    ('10A2', 10, 'A102'),
    ('10A3', 10, 'A103'),
    ('11A1', 11, 'A201'),
    ('11A2', 11, 'A202'),
    ('11A3', 11, 'A203'),
    ('12A1', 12, 'A301'),
    ('12A2', 12, 'A302'),
    ('12A3', 12, 'A303')
) AS class_data(name, grade_number, room_name)
WHERE sy.name = '2026-2027';

INSERT INTO users (phone, username, password_hash, full_name, address, role, status)
VALUES ('0900000001',
        'admin@fschool.edu.vn',
        '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
        'Demo Admin',
        NULL,
        'ADMIN',
        'ACTIVE');

WITH ordered_classes AS (
    SELECT c.id,
           c.name,
           c.grade_number,
           row_number() OVER (ORDER BY c.grade_number, c.name) AS class_no
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
),
student_data AS (
    SELECT ordered_classes.id AS class_id,
           ordered_classes.name AS class_name,
           ordered_classes.grade_number,
           ordered_classes.class_no,
           student_no,
           ((ordered_classes.class_no - 1) * 20 + student_no) AS global_no
    FROM ordered_classes
    CROSS JOIN generate_series(1, 20) AS series(student_no)
)
INSERT INTO users (
    class_id,
    phone,
    username,
    password_hash,
    student_code,
    full_name,
    date_of_birth,
    gender,
    address,
    guardian_name,
    guardian_phone,
    role,
    status
)
SELECT class_id,
       '0911' || lpad(global_no::text, 6, '0'),
       'student' || lpad(global_no::text, 3, '0') || '@fschool.edu.vn',
       '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
       'HS' || lpad(global_no::text, 4, '0'),
       'Hoc Sinh ' || class_name || ' ' || lpad(student_no::text, 2, '0'),
       make_date((2026 - grade_number - 5), ((student_no - 1) % 12) + 1, ((student_no - 1) % 28) + 1),
       CASE WHEN student_no % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
       'Ha Noi',
       'Phu Huynh ' || lpad(global_no::text, 3, '0'),
       '0988' || lpad(global_no::text, 6, '0'),
       'STUDENT',
       'ACTIVE'
FROM student_data;

WITH students AS (
    SELECT u.id,
           u.student_code,
           row_number() OVER (ORDER BY u.student_code) AS student_no
    FROM users u
    WHERE u.role = 'STUDENT'
),
grade_items AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 1.00, DATE '2026-09-10'),
        (1, 'MATH', 'GK', 'Diem giua ky', 'MIDTERM', 2.00, DATE '2026-10-20'),
        (1, 'MATH', 'CK', 'Diem cuoi ky', 'FINAL', 3.00, DATE '2026-12-20'),
        (1, 'TECHNOLOGY', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 1.00, DATE '2026-09-15'),
        (1, 'TECHNOLOGY', 'GK', 'Diem giua ky', 'MIDTERM', 2.00, DATE '2026-10-25'),
        (1, 'TECHNOLOGY', 'CK', 'Diem cuoi ky', 'FINAL', 3.00, DATE '2026-12-22'),
        (2, 'MATH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 1.00, DATE '2027-01-20'),
        (2, 'MATH', 'GK', 'Diem giua ky', 'MIDTERM', 2.00, DATE '2027-03-15'),
        (2, 'MATH', 'CK', 'Diem cuoi ky', 'FINAL', 3.00, DATE '2027-05-18'),
        (2, 'TECHNOLOGY', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 1.00, DATE '2027-01-25'),
        (2, 'TECHNOLOGY', 'GK', 'Diem giua ky', 'MIDTERM', 2.00, DATE '2027-03-20'),
        (2, 'TECHNOLOGY', 'CK', 'Diem cuoi ky', 'FINAL', 3.00, DATE '2027-05-20')
    ) AS data(semester_no, subject_code, component_code, title, grade_type, weight, assessment_date)
)
INSERT INTO grades (user_id, subject_id, semester_id, title, component_code, grade_type, score, weight, max_score, comment, assessment_date)
SELECT students.id,
       subjects.id,
       semesters.id,
       grade_items.title,
       grade_items.component_code,
       grade_items.grade_type,
       round((6 + (((students.student_no + grade_items.semester_no + length(grade_items.subject_code) + length(grade_items.component_code)) % 40) / 10.0))::numeric, 2),
       grade_items.weight,
       10.00,
       NULL,
       grade_items.assessment_date
FROM students
CROSS JOIN grade_items
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters ON semesters.school_year_id = sy.id AND semesters.semester_no = grade_items.semester_no
JOIN subjects ON subjects.code = grade_items.subject_code;

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
           U&'Ph\1EE5 huynh g\1EEDi \0111\01A1n ngh\1EC9 h\1ECDc c\00F3 ph\00E9p cho h\1ECDc sinh.',
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
       );

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
        'GV001',
        'Giao Vien 01',
        '0922000001',
        'teacher001@fschool.edu.vn',
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
        'GV002',
        'Giao Vien 02',
        '0922000002',
        'teacher002@fschool.edu.vn',
        U&'S\00E2n b\00F3ng',
        U&'Th\1EE9 6',
        TIME '16:00',
        TIME '17:30',
        35,
        45,
        TRUE,
        20);

WITH first_student AS (
    SELECT id
    FROM users
    WHERE student_code = 'HS0001'
),
first_club AS (
    SELECT id
    FROM clubs
    WHERE public_id = 'CLB001'
)
INSERT INTO club_registrations (student_id, club_id, status, reason, registered_at, approved_at)
SELECT first_student.id,
       first_club.id,
       'JOINED',
       'Em muon tham gia de ren luyen ky nang va giao luu voi cac ban.',
       TIMESTAMPTZ '2026-07-01 08:30:00+07',
       TIMESTAMPTZ '2026-07-02 09:00:00+07'
FROM first_student
CROSS JOIN first_club;

COMMIT;
