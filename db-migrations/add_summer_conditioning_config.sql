-- Summer Conditioning Config - Run manually against the uaxc MySQL database
-- Stores the per-year date window for the summer conditioning pride points award.
-- start_date: when XC season training begins counting
-- end_date:   camp cutoff date; points earned after this date do not count

CREATE TABLE IF NOT EXISTS uaxc.summer_conditioning_config (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  year             VARCHAR(10)  NOT NULL UNIQUE,
  start_date       DATE         NOT NULL,
  end_date         DATE         NOT NULL,
  points_threshold INT          NOT NULL DEFAULT 300,
  created_by       VARCHAR(100),
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Supports the summer conditioning date-range scan on point_transactions
-- Note: DROP first if re-running; CREATE INDEX IF NOT EXISTS requires MySQL 8.0.1+
CREATE INDEX idx_pt_type_created_at
  ON uaxc.point_transactions (transaction_type, created_at);
