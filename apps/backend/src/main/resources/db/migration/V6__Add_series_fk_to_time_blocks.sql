ALTER TABLE time_blocks
    ADD CONSTRAINT fk_time_blocks_series FOREIGN KEY (series_id) REFERENCES time_block_series(id);

CREATE UNIQUE INDEX uq_time_blocks_series_start ON time_blocks(series_id, start_datetime)
WHERE deleted_at IS NULL;
