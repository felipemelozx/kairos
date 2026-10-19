CREATE TABLE time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    start_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    end_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id UUID REFERENCES projects(id),
    series_id UUID,
    is_override BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_time_blocks_range CHECK (end_datetime > start_datetime)
);

CREATE INDEX idx_timeblocks_user_range ON time_blocks(user_id, start_datetime, end_datetime)
WHERE deleted_at IS NULL;
