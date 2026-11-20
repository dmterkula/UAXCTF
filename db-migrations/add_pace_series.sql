USE uaxc;

-- Pace time series, stored alongside the heart rate series for the same activity.
-- Purely additive: one nullable column. Rows without pace (older logs, Garmin without distance data,
-- indoor runs) simply have NULL here and clients hide the pace chart.
ALTER TABLE heart_rate_series
    ADD COLUMN pace_samples LONGTEXT NULL COMMENT 'JSON array of {offsetSeconds, secPerMile}';
