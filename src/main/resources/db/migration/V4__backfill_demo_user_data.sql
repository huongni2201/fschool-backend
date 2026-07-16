BEGIN;

UPDATE users
SET full_name = 'Demo Student',
    date_of_birth = DATE '2010-03-15',
    gender = 'MALE',
    address = 'Ha Noi',
    guardian_name = 'Demo Parent',
    guardian_phone = '0911000002',
    class_id = (
        SELECT c.id
        FROM classes c
        JOIN school_years sy ON sy.id = c.school_year_id
        WHERE sy.name = '2026-2027'
          AND c.name = '10A1'
    ),
    updated_at = now()
WHERE phone = '0911000001';

UPDATE users
SET full_name = 'Demo Parent',
    address = 'Ha Noi',
    updated_at = now()
WHERE phone = '0911000002';

UPDATE users
SET full_name = 'Demo Teacher',
    updated_at = now()
WHERE phone = '0911000003';

UPDATE teacher_profiles
SET full_name = 'Demo Teacher',
    department_name = 'Demo Department',
    updated_at = now()
WHERE user_id = (SELECT id FROM users WHERE phone = '0911000003');

WITH teacher AS (
    SELECT tp.id
    FROM teacher_profiles tp
    JOIN users u ON u.id = tp.user_id
    WHERE u.phone = '0911000003'
),
target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
      AND c.name = '10A1'
),
current_semester AS (
    SELECT sem.id
    FROM semesters sem
    JOIN school_years sy ON sy.id = sem.school_year_id
    WHERE sy.name = '2026-2027'
      AND sem.is_current = TRUE
),
target_subjects AS (
    SELECT id
    FROM subjects
    WHERE code IN ('MATH', 'LITERATURE', 'ENGLISH', 'PHYSICS', 'CHEMISTRY', 'BIOLOGY', 'HISTORY', 'GEOGRAPHY')
)
INSERT INTO teaching_assignments (teacher_id, class_id, subject_id, semester_id, is_active)
SELECT teacher.id,
       target_class.id,
       target_subjects.id,
       current_semester.id,
       TRUE
FROM teacher
CROSS JOIN target_class
CROSS JOIN current_semester
CROSS JOIN target_subjects
ON CONFLICT (teacher_id, class_id, subject_id, semester_id) DO UPDATE
SET is_active = TRUE,
    updated_at = now();

WITH teacher AS (
    SELECT tp.id
    FROM teacher_profiles tp
    JOIN users u ON u.id = tp.user_id
    WHERE u.phone = '0911000003'
),
target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
      AND c.name = '10A1'
)
INSERT INTO class_teacher_assignments (teacher_id, class_id, role, is_active)
SELECT teacher.id,
       target_class.id,
       'HOMEROOM_TEACHER',
       TRUE
FROM teacher
CROSS JOIN target_class
ON CONFLICT (teacher_id, class_id, role) DO UPDATE
SET is_active = TRUE,
    updated_at = now();

UPDATE classes
SET homeroom_teacher_name = 'Demo Teacher',
    updated_at = now()
WHERE id = (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    WHERE sy.name = '2026-2027'
      AND c.name = '10A1'
);

DELETE FROM grades
WHERE user_id = (SELECT id FROM users WHERE phone = '0911000001');

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

DELETE FROM request_histories
WHERE request_id IN (
    SELECT id
    FROM student_requests
    WHERE request_number IN ('REQ-DEMO-001', 'REQ-DEMO-002')
);

DELETE FROM student_requests
WHERE request_number IN ('REQ-DEMO-001', 'REQ-DEMO-002');

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
       'PROCESSING',
       '{"purpose":"Bo sung ho so ca nhan"}'::jsonb,
       TIMESTAMPTZ '2026-09-10 09:00:00+07',
       TIMESTAMPTZ '2026-09-11 10:00:00+07'
FROM target_user
CROSS JOIN confirmation_type;

INSERT INTO request_histories (request_id, status, note, created_by, created_at, updated_at)
SELECT sr.id,
       sr.status,
       CASE sr.status
           WHEN 'SUBMITTED' THEN 'Student submitted request'
           ELSE 'Teacher is reviewing request'
       END,
       sr.student_id,
       sr.created_at,
       sr.updated_at
FROM student_requests sr
WHERE sr.request_number IN ('REQ-DEMO-001', 'REQ-DEMO-002');

DELETE FROM notifications
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE phone IN ('0911000001', '0911000002', '0911000003')
)
AND title IN (
    'Diem moi da duoc cap nhat',
    'Lich thi sap toi',
    'Don xin nghi hoc dang cho duyet',
    'Thong bao hop phu huynh',
    'Hoc sinh co don can xu ly',
    'Lich kiem tra lop 10A1'
);

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
SELECT id, 'Don xin nghi hoc dang cho duyet', 'Don xin nghi hoc cua hoc sinh dang cho giao vien chu nhiem xu ly.', 'REQUEST', '/requests', FALSE, TIMESTAMPTZ '2026-09-18 08:45:00+07'
FROM demo_users
WHERE phone = '0911000002'
UNION ALL
SELECT id, 'Thong bao hop phu huynh', 'Nha truong moi phu huynh tham du buoi hop lop 10A1.', 'EVENT', '/parents/me', FALSE, TIMESTAMPTZ '2026-09-01 10:00:00+07'
FROM demo_users
WHERE phone = '0911000002'
UNION ALL
SELECT id, 'Hoc sinh co don can xu ly', 'Lop chu nhiem co don moi can giao vien xem xet.', 'APPLICATION', '/teacher/dashboard', FALSE, TIMESTAMPTZ '2026-09-18 09:00:00+07'
FROM demo_users
WHERE phone = '0911000003'
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
CROSS JOIN target_club
ON CONFLICT (student_id, club_id) DO UPDATE
SET status = EXCLUDED.status,
    reason = EXCLUDED.reason,
    registered_at = EXCLUDED.registered_at,
    approved_at = EXCLUDED.approved_at,
    cancelled_at = NULL,
    updated_at = now();

COMMIT;
