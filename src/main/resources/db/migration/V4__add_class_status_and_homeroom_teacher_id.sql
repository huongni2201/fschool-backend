ALTER TABLE classes
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE classes
    ADD CONSTRAINT ck_classes_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'));

ALTER TABLE classes
    ADD COLUMN homeroom_teacher_id UUID
        REFERENCES users (id)
            ON DELETE SET NULL;

UPDATE classes c
SET homeroom_teacher_id = teacher.user_id
FROM teacher_profiles teacher
WHERE lower(teacher.full_name) = lower(c.homeroom_teacher_name);
