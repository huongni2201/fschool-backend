ALTER TABLE subjects
    ADD COLUMN grade_levels VARCHAR(20) NOT NULL DEFAULT '10,11,12';

ALTER TABLE subjects
    ADD COLUMN lessons_per_week SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE subjects
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE subjects
    ADD CONSTRAINT ck_subjects_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE subjects
    ADD CONSTRAINT ck_subjects_lessons_per_week
        CHECK (lessons_per_week >= 0);

UPDATE subjects
SET lessons_per_week = CASE code
    WHEN 'MATH' THEN 4
    WHEN 'LITERATURE' THEN 4
    WHEN 'ENGLISH' THEN 3
    WHEN 'PHYSICS' THEN 2
    WHEN 'CHEMISTRY' THEN 2
    WHEN 'BIOLOGY' THEN 2
    WHEN 'INFORMATICS' THEN 1
    WHEN 'HISTORY' THEN 2
    WHEN 'GEOGRAPHY' THEN 2
    WHEN 'CIVICS' THEN 1
    WHEN 'TECHNOLOGY' THEN 1
    WHEN 'PE' THEN 2
    WHEN 'DEFENSE' THEN 1
    WHEN 'LOCAL_EDU' THEN 1
    ELSE lessons_per_week
END;
