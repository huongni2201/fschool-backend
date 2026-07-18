BEGIN;

INSERT INTO roles (code, display_name, description)
VALUES ('STUDENT', 'Student', 'Student account'),
       ('PARENT', 'Parent', 'Parent or guardian account'),
       ('TEACHER', 'Teacher', 'Teacher account'),
       ('ADMIN', 'Admin', 'Administrator account');

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

INSERT INTO classes (school_year_id, name, grade_number, homeroom_teacher_name, room_name)
SELECT sy.id,
       class_data.name,
       class_data.grade_number,
       class_data.homeroom_teacher_name,
       class_data.room_name
FROM school_years sy
CROSS JOIN (VALUES ('10A1', 10, 'Demo Teacher', 'A101'),
                   ('10A2', 10, NULL, 'A102'),
                   ('11A1', 11, NULL, 'A201'),
                   ('11A2', 11, NULL, 'A202'),
                   ('12A1', 12, NULL, 'A301'),
                   ('12A2', 12, NULL, 'A302')) AS class_data(name, grade_number, homeroom_teacher_name, room_name)
WHERE sy.name = '2026-2027';

WITH target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
      AND c.name = '10A1'
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
SELECT target_class.id,
       '0911000001',
       'student',
       '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
       'HS0001',
       'Demo Student',
       DATE '2010-03-15',
       'MALE',
       'Ha Noi',
       'Demo Parent',
       '0911000002',
       'STUDENT',
       'ACTIVE'
FROM target_class;

INSERT INTO users (phone, username, password_hash, full_name, address, role, status)
VALUES ('0911000002',
        'parent',
        '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
        'Demo Parent',
        'Ha Noi',
        'PARENT',
        'ACTIVE'),
       ('0911000003',
        'teacher',
        '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
        'Demo Teacher',
        NULL,
        'TEACHER',
        'ACTIVE'),
       ('0911000004',
        'admin',
        '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
        'Demo Admin',
        NULL,
        'ADMIN',
        'ACTIVE');

INSERT INTO teacher_profiles (user_id, employee_code, full_name, department_name)
SELECT u.id,
       'GVDEMO',
       u.full_name,
       'Demo Department'
FROM users u
WHERE u.phone = '0911000003';

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

WITH target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
      AND c.name = '10A1'
),
timetable_data AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH', 1, 1, TIME '07:00', TIME '07:45', 'Tran Thi Toan', 'A101'),
        (1, 'MATH', 1, 2, TIME '07:50', TIME '08:35', 'Tran Thi Toan', 'A101'),
        (1, 'ENGLISH', 1, 3, TIME '08:50', TIME '09:35', 'Le Thi Anh', 'A101'),
        (1, 'LITERATURE', 1, 4, TIME '09:40', TIME '10:25', 'Pham Van Ngu', 'A101'),
        (1, 'PHYSICS', 2, 1, TIME '07:00', TIME '07:45', 'Nguyen Van Ly', 'A101'),
        (1, 'CHEMISTRY', 2, 2, TIME '07:50', TIME '08:35', 'Hoang Thi Hoa', 'A101'),
        (1, 'BIOLOGY', 2, 3, TIME '08:50', TIME '09:35', 'Do Van Sinh', 'A101'),
        (1, 'INFORMATICS', 3, 1, TIME '07:00', TIME '07:45', 'Vu Thi Tin', 'Lab 1'),
        (1, 'HISTORY', 3, 2, TIME '07:50', TIME '08:35', 'Bui Van Su', 'A101'),
        (1, 'GEOGRAPHY', 4, 1, TIME '07:00', TIME '07:45', 'Dang Thi Dia', 'A101'),
        (1, 'CIVICS', 4, 2, TIME '07:50', TIME '08:35', 'Mai Van Cong', 'A101'),
        (1, 'PE', 5, 1, TIME '07:00', TIME '07:45', 'Phan Van The', 'San truong'),
        (1, 'DEFENSE', 5, 2, TIME '07:50', TIME '08:35', 'Cao Van Quoc', 'San truong'),
        (2, 'MATH', 1, 1, TIME '07:00', TIME '07:45', 'Tran Thi Toan', 'A101'),
        (2, 'LITERATURE', 1, 2, TIME '07:50', TIME '08:35', 'Pham Van Ngu', 'A101'),
        (2, 'ENGLISH', 1, 3, TIME '08:50', TIME '09:35', 'Le Thi Anh', 'A101'),
        (2, 'PHYSICS', 2, 1, TIME '07:00', TIME '07:45', 'Nguyen Van Ly', 'A101'),
        (2, 'CHEMISTRY', 2, 2, TIME '07:50', TIME '08:35', 'Hoang Thi Hoa', 'A101'),
        (2, 'TECHNOLOGY', 3, 1, TIME '07:00', TIME '07:45', 'Dinh Van Nghe', 'A101'),
        (2, 'LOCAL_EDU', 3, 2, TIME '07:50', TIME '08:35', 'Nguyen Thi Phuong', 'A101'),
        (2, 'HISTORY', 4, 1, TIME '07:00', TIME '07:45', 'Bui Van Su', 'A101'),
        (2, 'GEOGRAPHY', 4, 2, TIME '07:50', TIME '08:35', 'Dang Thi Dia', 'A101'),
        (2, 'PE', 5, 1, TIME '07:00', TIME '07:45', 'Phan Van The', 'San truong')
    ) AS data(semester_no, subject_code, day_of_week, period_no, start_time, end_time, teacher_name, room_name)
)
INSERT INTO timetable_entries (class_id, semester_id, subject_id, day_of_week, period_no, start_time, end_time, teacher_name, room_name)
SELECT target_class.id,
       sem.id,
       sub.id,
       timetable_data.day_of_week,
       timetable_data.period_no,
       timetable_data.start_time,
       timetable_data.end_time,
       timetable_data.teacher_name,
       timetable_data.room_name
FROM timetable_data
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = timetable_data.semester_no
JOIN subjects sub ON sub.code = timetable_data.subject_code
CROSS JOIN target_class;

WITH target_user AS (
    SELECT id
    FROM users
    WHERE phone = '0911000001'
),
grade_data AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.50, 1.00, DATE '2026-09-10', 'Lam bai tot'),
        (1, 'MATH', 'GK', 'Diem giua ky', 'MIDTERM', 7.75, 2.00, DATE '2026-10-20', NULL),
        (1, 'MATH', 'CK', 'Diem cuoi ky', 'FINAL', 8.00, 3.00, DATE '2026-12-20', NULL),
        (1, 'LITERATURE', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.00, 1.00, DATE '2026-09-12', NULL),
        (1, 'LITERATURE', 'GK', 'Diem giua ky', 'MIDTERM', 7.50, 2.00, DATE '2026-10-22', NULL),
        (1, 'ENGLISH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 9.00, 1.00, DATE '2026-09-15', 'Phat am tot'),
        (1, 'ENGLISH', 'GK', 'Diem giua ky', 'MIDTERM', 8.25, 2.00, DATE '2026-10-24', NULL),
        (1, 'PHYSICS', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 7.25, 1.00, DATE '2026-09-18', NULL),
        (1, 'CHEMISTRY', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.00, 1.00, DATE '2026-09-20', NULL),
        (2, 'MATH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.75, 1.00, DATE '2027-01-20', NULL),
        (2, 'MATH', 'GK', 'Diem giua ky', 'MIDTERM', 8.25, 2.00, DATE '2027-03-15', NULL),
        (2, 'MATH', 'CK', 'Diem cuoi ky', 'FINAL', 8.50, 3.00, DATE '2027-05-18', NULL),
        (2, 'ENGLISH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 9.25, 1.00, DATE '2027-01-25', 'Tu vung tot'),
        (2, 'PHYSICS', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 7.75, 1.00, DATE '2027-01-28', NULL)
    ) AS data(semester_no, subject_code, component_code, title, grade_type, score, weight, assessment_date, comment)
)
INSERT INTO grades (user_id, subject_id, semester_id, title, component_code, grade_type, score, weight, max_score, comment, assessment_date)
SELECT target_user.id,
       sub.id,
       sem.id,
       grade_data.title,
       grade_data.component_code,
       grade_data.grade_type,
       grade_data.score,
       grade_data.weight,
       10.00,
       grade_data.comment,
       grade_data.assessment_date
FROM grade_data
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = grade_data.semester_no
JOIN subjects sub ON sub.code = grade_data.subject_code
CROSS JOIN target_user;

WITH target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
      AND c.name = '10A1'
),
exam_data AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH', 'Kiem tra giua ky Toan', 'MIDTERM', DATE '2026-10-20', TIME '07:30', 90, 'A101', 'Mang may tinh cam tay.'),
        (1, 'ENGLISH', 'Kiem tra giua ky Tieng Anh', 'MIDTERM', DATE '2026-10-24', TIME '09:30', 60, 'A101', NULL),
        (1, 'LITERATURE', 'Kiem tra giua ky Ngu van', 'MIDTERM', DATE '2026-10-22', TIME '07:30', 90, 'A101', NULL),
        (1, 'MATH', 'Kiem tra cuoi ky Toan', 'FINAL', DATE '2026-12-20', TIME '07:30', 90, 'A101', NULL),
        (1, 'ENGLISH', 'Kiem tra cuoi ky Tieng Anh', 'FINAL', DATE '2026-12-24', TIME '09:30', 60, 'A101', NULL),
        (2, 'MATH', 'Kiem tra giua ky Toan', 'MIDTERM', DATE '2027-03-15', TIME '07:30', 90, 'A101', 'Mang may tinh cam tay.'),
        (2, 'LITERATURE', 'Kiem tra giua ky Ngu van', 'MIDTERM', DATE '2027-03-17', TIME '07:30', 90, 'A101', NULL),
        (2, 'ENGLISH', 'Kiem tra giua ky Tieng Anh', 'MIDTERM', DATE '2027-03-19', TIME '09:30', 60, 'A101', NULL),
        (2, 'MATH', 'Kiem tra cuoi ky Toan', 'FINAL', DATE '2027-05-18', TIME '07:30', 90, 'A101', NULL),
        (2, 'LITERATURE', 'Kiem tra cuoi ky Ngu van', 'FINAL', DATE '2027-05-20', TIME '07:30', 90, 'A101', NULL),
        (2, 'ENGLISH', 'Kiem tra cuoi ky Tieng Anh', 'FINAL', DATE '2027-05-22', TIME '09:30', 60, 'A101', NULL)
    ) AS data(semester_no, subject_code, title, exam_type, exam_date, start_time, duration_minutes, room_name, note)
)
INSERT INTO exams (class_id, subject_id, semester_id, title, exam_type, exam_date, start_time, duration_minutes, room_name, note)
SELECT target_class.id,
       sub.id,
       sem.id,
       exam_data.title,
       exam_data.exam_type,
       exam_data.exam_date,
       exam_data.start_time,
       exam_data.duration_minutes,
       exam_data.room_name,
       exam_data.note
FROM exam_data
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = exam_data.semester_no
JOIN subjects sub ON sub.code = exam_data.subject_code
CROSS JOIN target_class;

WITH target_user AS (
    SELECT id
    FROM users
    WHERE phone = '0911000001'
),
absence_type AS (
    SELECT id
    FROM request_types
    WHERE code = 'absence'
),
confirmation_type AS (
    SELECT id
    FROM request_types
    WHERE code = 'confirmation'
)
INSERT INTO student_requests (request_number, student_id, type_id, title, status, form_data, created_at, updated_at)
SELECT 'REQ-DEMO-001',
       target_user.id,
       absence_type.id,
       'Don xin nghi hoc ngay 20/09',
       'SUBMITTED',
       '{"reason":"Em bi sot can nghi hoc mot ngay.","startDate":"2026-09-20","endDate":"2026-09-20"}'::jsonb,
       TIMESTAMPTZ '2026-09-18 08:30:00+07',
       TIMESTAMPTZ '2026-09-18 08:30:00+07'
FROM target_user
CROSS JOIN absence_type
UNION ALL
SELECT 'REQ-DEMO-002',
       target_user.id,
       confirmation_type.id,
       'Don xac nhan hoc sinh',
       'SUBMITTED',
       '{"purpose":"Bo sung ho so ca nhan"}'::jsonb,
       TIMESTAMPTZ '2026-09-10 09:00:00+07',
       TIMESTAMPTZ '2026-09-10 09:00:00+07'
FROM target_user
CROSS JOIN confirmation_type;

WITH parent_user AS (
    SELECT id
    FROM users
    WHERE phone = '0911000002'
)
INSERT INTO request_histories (request_id, status, note, created_by, created_at, updated_at)
SELECT student_requests.id,
       student_requests.status,
       'Parent submitted request',
       parent_user.id,
       student_requests.created_at,
       student_requests.updated_at
FROM student_requests
CROSS JOIN parent_user
WHERE student_requests.request_number IN ('REQ-DEMO-001', 'REQ-DEMO-002');

WITH demo_users AS (
    SELECT id, phone
    FROM users
    WHERE phone IN ('0911000001', '0911000002', '0911000003')
)
INSERT INTO notifications (user_id, title, body, notification_type, deep_link, is_read, created_at)
SELECT id, 'Diem moi da duoc cap nhat', 'Diem giua ky mon Toan va Tieng Anh da co tren he thong.', 'GRADE', '/grades', FALSE, TIMESTAMPTZ '2026-10-24 15:00:00+07'
FROM demo_users
WHERE phone = '0911000001'
UNION ALL
SELECT id, 'Lich thi sap toi', 'Lop 10A1 co lich kiem tra giua ky trong thang 10.', 'EXAM', '/exams', FALSE, TIMESTAMPTZ '2026-10-01 08:00:00+07'
FROM demo_users
WHERE phone = '0911000001'
UNION ALL
SELECT id, 'Don xin nghi hoc da duoc gui', 'Don xin nghi hoc cua hoc sinh da duoc phu huynh gui tren he thong.', 'REQUEST', '/requests', FALSE, TIMESTAMPTZ '2026-09-18 08:45:00+07'
FROM demo_users
WHERE phone = '0911000002'
UNION ALL
SELECT id, 'Thong bao hop phu huynh', 'Nha truong moi phu huynh tham du buoi hop lop 10A1.', 'EVENT', '/parents/me', FALSE, TIMESTAMPTZ '2026-09-01 10:00:00+07'
FROM demo_users
WHERE phone = '0911000002'
UNION ALL
SELECT id, 'Lich kiem tra lop 10A1', 'Co lich kiem tra sap toi cho cac lop phu trach.', 'EXAM', '/teacher/dashboard', FALSE, TIMESTAMPTZ '2026-10-01 08:15:00+07'
FROM demo_users
WHERE phone = '0911000003';

WITH target_user AS (
    SELECT id
    FROM users
    WHERE phone = '0911000001'
),
target_club AS (
    SELECT id
    FROM clubs
    WHERE public_id = 'CLB001'
)
INSERT INTO club_registrations (student_id, club_id, status, reason, registered_at, approved_at)
SELECT target_user.id,
       target_club.id,
       'JOINED',
       'Em muon tham gia de ren luyen ky nang va giao luu voi cac ban.',
       TIMESTAMPTZ '2026-07-01 08:30:00+07',
       TIMESTAMPTZ '2026-07-02 09:00:00+07'
FROM target_user
CROSS JOIN target_club;

COMMIT;
