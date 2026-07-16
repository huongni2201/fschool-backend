BEGIN;

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
FROM target_class
ON CONFLICT (phone) DO UPDATE
SET class_id = EXCLUDED.class_id,
    password_hash = EXCLUDED.password_hash,
    student_code = EXCLUDED.student_code,
    full_name = EXCLUDED.full_name,
    date_of_birth = EXCLUDED.date_of_birth,
    gender = EXCLUDED.gender,
    address = EXCLUDED.address,
    guardian_name = EXCLUDED.guardian_name,
    guardian_phone = EXCLUDED.guardian_phone,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO users (phone, password_hash, full_name, role, status)
VALUES ('0911000002',
        '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
        'Demo Parent',
        'PARENT',
        'ACTIVE')
ON CONFLICT (phone) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO users (phone, password_hash, full_name, role, status)
VALUES ('0911000003',
        '$2a$10$IqXzCiWhunJMtUFqQvVMMOQ2B3.ZlNRxbi5e3UhBNnOfMv6168zqi',
        'Demo Teacher',
        'TEACHER',
        'ACTIVE')
ON CONFLICT (phone) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO teacher_profiles (user_id, employee_code, full_name, department_name)
SELECT u.id,
       'GVDEMO',
       u.full_name,
       'Demo Department'
FROM users u
WHERE u.phone = '0911000003'
ON CONFLICT (user_id) DO UPDATE
SET employee_code = EXCLUDED.employee_code,
    full_name = EXCLUDED.full_name,
    department_name = EXCLUDED.department_name,
    updated_at = now();

COMMIT;
