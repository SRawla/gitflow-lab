CREATE TABLE user_course (
    user_id    UUID NOT NULL REFERENCES user_detail(id) ON DELETE CASCADE,
    course_id  UUID NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, course_id)
);
