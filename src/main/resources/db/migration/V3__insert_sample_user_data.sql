BEGIN;

CREATE TEMP TABLE sample_user_context
(
    student_code VARCHAR(20) NOT NULL,
    class_name   VARCHAR(20) NOT NULL
) ON COMMIT DROP;

-- Change student_code to the value created through the register API.
INSERT INTO sample_user_context (student_code, class_name)
VALUES ('HS0001', '10A1');

WITH target_user AS (
    SELECT u.id
    FROM users u
    JOIN sample_user_context ctx ON ctx.student_code = u.student_code
),
target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    JOIN sample_user_context ctx ON ctx.class_name = c.name
    WHERE sy.name = '2026-2027'
)
UPDATE users
SET class_id = (SELECT id FROM target_class),
    date_of_birth = COALESCE(date_of_birth, DATE '2010-03-15'),
    gender = COALESCE(gender, 'MALE'),
    address = COALESCE(address, 'Ha Noi'),
    guardian_name = COALESCE(guardian_name, 'Nguyen Van B'),
    guardian_phone = COALESCE(guardian_phone, '0912345678'),
    updated_at = now()
WHERE id = (SELECT id FROM target_user);

WITH target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    JOIN sample_user_context ctx ON ctx.class_name = c.name
    WHERE sy.name = '2026-2027'
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
SELECT tc.id,
       sem.id,
       sub.id,
       td.day_of_week,
       td.period_no,
       td.start_time,
       td.end_time,
       td.teacher_name,
       td.room_name
FROM timetable_data td
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = td.semester_no
JOIN subjects sub ON sub.code = td.subject_code
CROSS JOIN target_class tc
ON CONFLICT (class_id, semester_id, day_of_week, period_no) DO UPDATE
SET subject_id = EXCLUDED.subject_id,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    teacher_name = EXCLUDED.teacher_name,
    room_name = EXCLUDED.room_name,
    updated_at = now();

WITH target_user AS (
    SELECT u.id
    FROM users u
    JOIN sample_user_context ctx ON ctx.student_code = u.student_code
),
grade_data AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.50, 1.00, DATE '2026-09-10', 'Lam bai tot'),
        (1, 'MATH', 'GK', 'Diem giua ky', 'MIDTERM', 7.75, 2.00, DATE '2026-10-20', NULL),
        (1, 'MATH', 'CK', 'Diem cuoi ky', 'FINAL', 8.00, 3.00, DATE '2026-12-20', NULL),
        (1, 'LITERATURE', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.00, 1.00, DATE '2026-09-12', NULL),
        (1, 'LITERATURE', 'GK', 'Diem giua ky', 'MIDTERM', 7.50, 2.00, DATE '2026-10-22', NULL),
        (1, 'LITERATURE', 'CK', 'Diem cuoi ky', 'FINAL', 8.25, 3.00, DATE '2026-12-22', NULL),
        (1, 'ENGLISH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 9.00, 1.00, DATE '2026-09-15', 'Phat am tot'),
        (1, 'ENGLISH', 'GK', 'Diem giua ky', 'MIDTERM', 8.25, 2.00, DATE '2026-10-24', NULL),
        (1, 'ENGLISH', 'CK', 'Diem cuoi ky', 'FINAL', 8.75, 3.00, DATE '2026-12-24', NULL),
        (1, 'PHYSICS', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 7.25, 1.00, DATE '2026-09-18', NULL),
        (1, 'PHYSICS', 'GK', 'Diem giua ky', 'MIDTERM', 7.50, 2.00, DATE '2026-10-26', NULL),
        (1, 'CHEMISTRY', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.00, 1.00, DATE '2026-09-20', NULL),
        (2, 'MATH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.75, 1.00, DATE '2027-01-20', NULL),
        (2, 'MATH', 'GK', 'Diem giua ky', 'MIDTERM', 8.25, 2.00, DATE '2027-03-15', NULL),
        (2, 'MATH', 'CK', 'Diem cuoi ky', 'FINAL', 8.50, 3.00, DATE '2027-05-18', NULL),
        (2, 'LITERATURE', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.25, 1.00, DATE '2027-01-22', NULL),
        (2, 'LITERATURE', 'GK', 'Diem giua ky', 'MIDTERM', 8.00, 2.00, DATE '2027-03-17', NULL),
        (2, 'LITERATURE', 'CK', 'Diem cuoi ky', 'FINAL', 8.25, 3.00, DATE '2027-05-20', NULL),
        (2, 'ENGLISH', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 9.25, 1.00, DATE '2027-01-25', 'Tu vung tot'),
        (2, 'ENGLISH', 'GK', 'Diem giua ky', 'MIDTERM', 8.75, 2.00, DATE '2027-03-19', NULL),
        (2, 'ENGLISH', 'CK', 'Diem cuoi ky', 'FINAL', 9.00, 3.00, DATE '2027-05-22', NULL),
        (2, 'PHYSICS', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 7.75, 1.00, DATE '2027-01-28', NULL),
        (2, 'CHEMISTRY', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.25, 1.00, DATE '2027-02-02', NULL),
        (2, 'TECHNOLOGY', 'TX1', 'Diem thuong xuyen 1', 'REGULAR', 8.50, 1.00, DATE '2027-02-05', NULL)
    ) AS data(semester_no, subject_code, component_code, title, grade_type, score, weight, assessment_date, comment)
)
INSERT INTO grades (user_id, subject_id, semester_id, title, component_code, grade_type, score, weight, max_score, comment, assessment_date)
SELECT tu.id,
       sub.id,
       sem.id,
       gd.title,
       gd.component_code,
       gd.grade_type,
       gd.score,
       gd.weight,
       10.00,
       gd.comment,
       gd.assessment_date
FROM grade_data gd
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = gd.semester_no
JOIN subjects sub ON sub.code = gd.subject_code
CROSS JOIN target_user tu;

WITH target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    JOIN sample_user_context ctx ON ctx.class_name = c.name
    WHERE sy.name = '2026-2027'
),
assignment_data AS (
    SELECT *
    FROM (VALUES
        (1, 'MATH', 'Bai tap ham so bac nhat', 'Hoan thanh bai 1 den bai 8 trang 32.', 'Tran Thi Toan', TIMESTAMPTZ '2026-09-25 23:59:00+07'),
        (1, 'ENGLISH', 'Writing task unit 2', 'Write a 150-word paragraph about your school.', 'Le Thi Anh', TIMESTAMPTZ '2026-09-28 23:59:00+07'),
        (1, 'LITERATURE', 'Soan bai Truyen Kieu', 'Doc truoc van ban va tra loi cau hoi phan doc hieu.', 'Pham Van Ngu', TIMESTAMPTZ '2026-10-02 23:59:00+07'),
        (2, 'MATH', 'Bai tap phuong trinh bac hai', 'Hoan thanh bai 1 den bai 10 trang 76.', 'Tran Thi Toan', TIMESTAMPTZ '2027-02-12 23:59:00+07'),
        (2, 'ENGLISH', 'Speaking task unit 7', 'Prepare a 2-minute talk about future plans.', 'Le Thi Anh', TIMESTAMPTZ '2027-02-18 23:59:00+07'),
        (2, 'TECHNOLOGY', 'Bao cao du an cong nghe', 'Nop ban mo ta y tuong san pham nho cua nhom.', 'Dinh Van Nghe', TIMESTAMPTZ '2027-03-05 23:59:00+07')
    ) AS data(semester_no, subject_code, title, description, teacher_name, due_at)
)
INSERT INTO assignments (class_id, subject_id, semester_id, title, description, teacher_name, due_at, status)
SELECT tc.id,
       sub.id,
       sem.id,
       ad.title,
       ad.description,
       ad.teacher_name,
       ad.due_at,
       'PUBLISHED'
FROM assignment_data ad
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = ad.semester_no
JOIN subjects sub ON sub.code = ad.subject_code
CROSS JOIN target_class tc;

WITH target_class AS (
    SELECT c.id
    FROM classes c
    JOIN school_years sy ON sy.id = c.school_year_id
    JOIN sample_user_context ctx ON ctx.class_name = c.name
    WHERE sy.name = '2026-2027'
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
SELECT tc.id,
       sub.id,
       sem.id,
       ed.title,
       ed.exam_type,
       ed.exam_date,
       ed.start_time,
       ed.duration_minutes,
       ed.room_name,
       ed.note
FROM exam_data ed
JOIN school_years sy ON sy.name = '2026-2027'
JOIN semesters sem ON sem.school_year_id = sy.id AND sem.semester_no = ed.semester_no
JOIN subjects sub ON sub.code = ed.subject_code
CROSS JOIN target_class tc;

WITH target_user AS (
    SELECT u.id
    FROM users u
    JOIN sample_user_context ctx ON ctx.student_code = u.student_code
)
INSERT INTO notifications (user_id, title, body, notification_type, deep_link)
SELECT tu.id,
       notification_data.title,
       notification_data.body,
       notification_data.notification_type,
       notification_data.deep_link
FROM (VALUES ('Cap nhat diem giua ky', 'Diem giua ky mot so mon hoc da duoc cap nhat.', 'GRADE', '/grades'),
             ('Lich thi sap toi', 'Ban co lich kiem tra giua ky trong thang 10.', 'EXAM', '/exams'),
             ('Bai tap moi', 'Lop 10A1 co bai tap moi can hoan thanh.', 'ASSIGNMENT', '/assignments'),
             ('Thoi khoa bieu hoc ky 2', 'Thoi khoa bieu hoc ky 2 da san sang.', 'TIMETABLE', '/timetable')) AS notification_data(title, body, notification_type, deep_link)
CROSS JOIN target_user tu;

COMMIT;
