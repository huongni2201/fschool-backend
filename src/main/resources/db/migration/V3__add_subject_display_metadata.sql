BEGIN;

ALTER TABLE subjects
    ADD COLUMN IF NOT EXISTS subject_group VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN IF NOT EXISTS accent_color VARCHAR(20) NOT NULL DEFAULT '#64748B';

UPDATE subjects
SET subject_group = subject_data.subject_group,
    accent_color = subject_data.accent_color,
    updated_at = now()
FROM (VALUES ('MATH', 'NATURAL', '#F46A00'),
             ('LITERATURE', 'SOCIAL', '#B07D56'),
             ('ENGLISH', 'FOREIGN_LANGUAGE', '#2563EB'),
             ('PHYSICS', 'NATURAL', '#0EA5E9'),
             ('CHEMISTRY', 'NATURAL', '#16A34A'),
             ('BIOLOGY', 'NATURAL', '#65A30D'),
             ('INFORMATICS', 'NATURAL', '#7C3AED'),
             ('HISTORY', 'SOCIAL', '#B45309'),
             ('GEOGRAPHY', 'SOCIAL', '#0891B2'),
             ('CIVICS', 'SOCIAL', '#DB2777'),
             ('TECHNOLOGY', 'NATURAL', '#475569'),
             ('PE', 'SKILL', '#DC2626'),
             ('DEFENSE', 'SKILL', '#4B5563'),
             ('LOCAL_EDU', 'SOCIAL', '#059669')) AS subject_data(code, subject_group, accent_color)
WHERE subjects.code = subject_data.code;

COMMIT;
