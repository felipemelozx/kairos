CREATE TABLE time_block_series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    project_id UUID REFERENCES projects(id),
    start_date DATE NOT NULL,
    end_date DATE,
    pattern JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_series_range CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_series_user_active ON time_block_series(user_id)
WHERE deleted_at IS NULL;
