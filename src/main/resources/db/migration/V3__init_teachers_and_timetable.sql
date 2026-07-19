BEGIN;

WITH teacher_data AS (
    SELECT teacher_no,
           '0922' || lpad(teacher_no::text, 6, '0') AS phone,
           'teacher' || lpad(teacher_no::text, 3, '0') || '@fschool.edu.vn' AS username,
           'GV' || lpad(teacher_no::text, 3, '0') AS employee_code,
           'Giao Vien ' || lpad(teacher_no::text, 2, '0') AS full_name,
           CASE
               WHEN teacher_no IN (1, 2, 15, 16) THEN 'To Toan'
               WHEN teacher_no IN (3, 17) THEN 'To Ngu van'
               WHEN teacher_no IN (4, 18) THEN 'To Tieng Anh'
               WHEN teacher_no IN (5, 6, 19, 20) THEN 'To Khoa hoc tu nhien'
               WHEN teacher_no IN (7, 8, 9) THEN 'To Khoa hoc xa hoi'
               ELSE 'To Ky nang'
           END AS department_name
    FROM generate_series(1, 20) AS series(teacher_no)
)
INSERT INTO users (phone, username, password_hash, full_name, address, role, status)
SELECT phone,
       username,
       '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
       full_name,
       'Ha Noi',
       'TEACHER',
       'ACTIVE'
FROM teacher_data;

WITH teacher_data AS (
    SELECT teacher_no,
           '0922' || lpad(teacher_no::text, 6, '0') AS phone,
           'GV' || lpad(teacher_no::text, 3, '0') AS employee_code,
           'Giao Vien ' || lpad(teacher_no::text, 2, '0') AS full_name,
           CASE
               WHEN teacher_no IN (1, 2, 15, 16) THEN 'To Toan'
               WHEN teacher_no IN (3, 17) THEN 'To Ngu van'
               WHEN teacher_no IN (4, 18) THEN 'To Tieng Anh'
               WHEN teacher_no IN (5, 6, 19, 20) THEN 'To Khoa hoc tu nhien'
               WHEN teacher_no IN (7, 8, 9) THEN 'To Khoa hoc xa hoi'
               ELSE 'To Ky nang'
           END AS department_name
    FROM generate_series(1, 20) AS series(teacher_no)
)
INSERT INTO teacher_profiles (user_id, employee_code, full_name, department_name)
SELECT users.id,
       teacher_data.employee_code,
       teacher_data.full_name,
       teacher_data.department_name
FROM teacher_data
JOIN users ON users.phone = teacher_data.phone;

WITH ordered_classes AS (
    SELECT c.id,
           c.name,
           row_number() OVER (ORDER BY c.grade_number, c.name) AS class_no
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
),
homeroom_teachers AS (
    SELECT users.id AS teacher_id,
           teacher_profiles.full_name,
           row_number() OVER (ORDER BY teacher_profiles.employee_code) AS class_no
    FROM teacher_profiles
    JOIN users ON users.id = teacher_profiles.user_id
    WHERE teacher_profiles.employee_code BETWEEN 'GV001' AND 'GV009'
)
UPDATE classes
SET homeroom_teacher_id = homeroom_teachers.teacher_id,
    homeroom_teacher_name = homeroom_teachers.full_name
FROM ordered_classes
JOIN homeroom_teachers ON homeroom_teachers.class_no = ordered_classes.class_no
WHERE classes.id = ordered_classes.id;

WITH teacher_subjects AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH'),
        (2, 'MATH'),
        (3, 'LITERATURE'),
        (4, 'ENGLISH'),
        (5, 'PHYSICS'),
        (6, 'CHEMISTRY'),
        (7, 'HISTORY'),
        (8, 'GEOGRAPHY'),
        (9, 'CIVICS'),
        (10, 'TECHNOLOGY'),
        (11, 'INFORMATICS'),
        (12, 'BIOLOGY'),
        (13, 'PE'),
        (14, 'DEFENSE'),
        (15, 'MATH'),
        (16, 'TECHNOLOGY'),
        (17, 'LITERATURE'),
        (18, 'ENGLISH'),
        (19, 'PHYSICS'),
        (20, 'CHEMISTRY')
    ) AS data(teacher_no, subject_code)
),
ordered_classes AS (
    SELECT c.id,
           c.name,
           c.room_name,
           row_number() OVER (ORDER BY c.grade_number, c.name) AS class_no
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
),
slots AS (
    SELECT ordered_classes.id AS class_id,
           ordered_classes.room_name,
           semesters.id AS semester_id,
           day_of_week,
           period_no,
           ((ordered_classes.class_no + semesters.semester_no + day_of_week + period_no - 4) % 20) + 1 AS teacher_no
    FROM ordered_classes
    JOIN school_years sy ON sy.name = '2026-2027'
    JOIN semesters ON semesters.school_year_id = sy.id
    CROSS JOIN generate_series(1, 5) AS days(day_of_week)
    CROSS JOIN generate_series(1, 4) AS periods(period_no)
),
resolved_slots AS (
    SELECT slots.class_id,
           slots.semester_id,
           slots.day_of_week,
           slots.period_no,
           CASE slots.period_no
               WHEN 1 THEN TIME '07:00'
               WHEN 2 THEN TIME '07:50'
               WHEN 3 THEN TIME '08:50'
               ELSE TIME '09:40'
           END AS start_time,
           CASE slots.period_no
               WHEN 1 THEN TIME '07:45'
               WHEN 2 THEN TIME '08:35'
               WHEN 3 THEN TIME '09:35'
               ELSE TIME '10:25'
           END AS end_time,
           slots.room_name,
           teacher_subjects.subject_code,
           teacher_profiles.user_id AS teacher_id,
           teacher_profiles.full_name AS teacher_name
    FROM slots
    JOIN teacher_subjects ON teacher_subjects.teacher_no = slots.teacher_no
    JOIN teacher_profiles ON teacher_profiles.employee_code = 'GV' || lpad(slots.teacher_no::text, 3, '0')
)
INSERT INTO timetable_entries (
    class_id,
    semester_id,
    subject_id,
    teacher_id,
    day_of_week,
    period_no,
    start_time,
    end_time,
    teacher_name,
    room_name
)
SELECT resolved_slots.class_id,
       resolved_slots.semester_id,
       subjects.id,
       resolved_slots.teacher_id,
       resolved_slots.day_of_week,
       resolved_slots.period_no,
       resolved_slots.start_time,
       resolved_slots.end_time,
       resolved_slots.teacher_name,
       resolved_slots.room_name
FROM resolved_slots
JOIN subjects ON subjects.code = resolved_slots.subject_code;

WITH demo_users AS (
    SELECT id, phone, student_code
    FROM users
    WHERE student_code IN ('HS0001', 'HS0021', 'HS0041')
       OR phone IN ('0922000001', '0900000001')
)
INSERT INTO notifications (user_id, title, body, notification_type, deep_link, is_read, created_at)
SELECT id, 'Diem moi da duoc cap nhat', 'Diem mon Toan va Cong nghe da co tren he thong.', 'GRADE', '/grades', FALSE, TIMESTAMPTZ '2026-10-24 15:00:00+07'
FROM demo_users
WHERE student_code = 'HS0001'
UNION ALL
SELECT id, 'Lich thi sap toi', 'Lop cua ban co lich kiem tra trong thang 10.', 'EXAM', '/exams', FALSE, TIMESTAMPTZ '2026-10-01 08:00:00+07'
FROM demo_users
WHERE student_code = 'HS0021'
UNION ALL
SELECT id, 'Lich day trong tuan', 'Ban co lich day moi tren he thong.', 'TIMETABLE', '/teacher/dashboard', FALSE, TIMESTAMPTZ '2026-09-01 10:00:00+07'
FROM demo_users
WHERE phone = '0922000001'
UNION ALL
SELECT id, 'Bao cao dau nam', 'Du lieu lop, hoc sinh va giao vien da duoc khoi tao.', 'ADMIN', '/admin/dashboard', FALSE, TIMESTAMPTZ '2026-08-15 08:00:00+07'
FROM demo_users
WHERE phone = '0900000001';

COMMIT;
