ALTER TABLE timetable_entries
    ADD COLUMN teacher_id UUID
        REFERENCES users (id)
            ON DELETE SET NULL;

UPDATE timetable_entries entry
SET teacher_id = teacher.user_id
FROM teacher_profiles teacher
WHERE lower(entry.teacher_name) = lower(teacher.full_name)
  AND (
      SELECT count(*)
      FROM teacher_profiles candidate
      WHERE lower(candidate.full_name) = lower(entry.teacher_name)
  ) = 1;

CREATE INDEX idx_timetable_teacher_semester_day
    ON timetable_entries (teacher_id, semester_id, day_of_week, start_time, period_no);
