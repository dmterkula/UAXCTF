# Pace Calibration — Feature Plan

## Goal

A coach-facing, per-runner readout that surfaces how each athlete's **actual** training/workout paces deviate from their **assigned** pace ranges — framed as a tool to **calibrate the coach's pace estimate**, not to judge athlete compliance.

### Guiding principle (why this exists)

Early in a season, a runner going faster or slower than their assigned range usually does **not** mean the athlete did something wrong — it means the coach's *base estimate* for that athlete is miscalibrated. We are still learning each athlete. As the season progresses and real race results come in, the estimate tightens and confidence rises. The same deviation therefore *means different things* depending on how much we trust the estimate:

- **Low confidence (early season):** deviation → *tune the estimate.*
- **High confidence (race-anchored, late season):** deviation → *signal about the athlete* (fatigue, fitness change).

The feature must stay calibration-positive in tone. No red/alarm framing while confidence is low.

---

## The data already exists

No new data capture is required — both the target and the actual are already stored.

### Two pace subsystems (both must be covered)

| | **Training runs** (recovery, base building) | **Workouts** (tempo, threshold, intervals) |
|---|---|---|
| Target derived from | `TrainingBasePerformance` (the runner's 5k **training base**) | a **race basis** — Goal / PR / SB / Season Avg for an event |
| How the % is applied | named saved zone (`effortLabel` → `BaseTrainingPercentage`), per-runner override via `RunnerBaseTrainingPercentage` | `WorkoutComponent.percent` (coach's mental model: 90% = threshold, 87.5% = tempo) |
| Actual captured | `avgPace` on the run | per-rep `splits` |
| Deviation today | derive from `avgPace` vs `paceRange` | **already computed**: `WorkoutSplitsDTO.avgDifferenceFromTarget` |
| Fetch path | `TrainingRunsService.getARunnersTrainingRunsByTypeWithinDates(runnerId, start, end, season)` → `RunnerTrainingRunDTO` (carries `paceRange`, `avgPace`, `effortLevel`, `painLevel`) | `WorkoutResultService` / `WorkoutSplitsDTO` |

**Important:** today only *recovery* and *base building* are saved as named training-base zones. Tempo / threshold / intervals exist **only** on the workout side (percent off a race basis). Phase 1 must ingest **both** subsystems or it will silently miss all tempo/threshold deviation.

---

## Core metric — relative deviation

Raw seconds/mile isn't comparable across zones (a 10 s/mi miss on a 6:00 tempo ≠ on a 9:00 recovery). Normalize:

```
expectedMid = midpoint(fastEndPace, slowEndPace)            // seconds/mile
relDev      = (actualPaceSec - expectedMidSec) / expectedMidSec   // signed fraction
            // negative = faster than target, positive = slower
```

Aggregate **per zone** (recovery, base building, tempo, threshold, …): `meanRelDev`, `n`, `stdDev(relDev)`.

### Eligibility filter (what counts as a calibration sample)

- Target range must exist (`paceRange != null`; training run had `useTrainingPercent == true`).
- Exclude athlete-flagged-off runs: high `painLevel` (default exclude `painLevel >= 4`) or effort contradicting the zone. Keeps "I felt awful today" out of estimate math. Configurable.

---

## The decision — which knob is wrong

The two-knob model generalizes across both subsystems:

| Knob | Training runs | Workouts |
|---|---|---|
| **Base** | training base (`TrainingBasePerformance`) | race basis (Goal/PR/SB/Season Avg) |
| **Zone %** | `RunnerBaseTrainingPercentage` | `WorkoutComponent.percent` |

Compare each zone's `meanRelDev` against the others:

- **Uniform** — all zones cluster within `calibration.uniform_band` (default 1.5%) **and** the shared mean exceeds `calibration.min_signal` (default 1.5%):
  → **base is off.** Suggested base = `currentBaseSeconds * (1 + sharedMeanRelDev)` (paces scale linearly with base). Surface as a suggested 5k base time.
- **Zone outlier** — one zone's `meanRelDev` deviates from the others by ≥ `calibration.zone_outlier_band` (default 2.5%):
  → **that zone's percentage is off for this runner.** Solve for the `percent` that recenters that zone on observed pace (pace ∝ 1/percent via the existing percentage mapping).
- **Neither** (within `min_signal`): "dialed in — estimate looks good."

---

## Sampling model

- **A sample = one logged session, not one rep.** A tempo workout with 8 reps is *one* sample (using its `avgDifferenceFromTarget`); the reps are correlated (same day/conditions/state) and counting them independently would fake confidence. One training run = one sample likewise.
- **Counted and judged per zone, not pooled** — so abundant recovery runs can't drown out a sparse tempo signal. Each zone gets its own sample count, verdict, and confidence.
- **Workout zones are more diagnostic than training-run zones** (athlete is targeting a pace under structured effort; recovery "too fast" is often just terrain/enthusiasm). So they saturate confidence on fewer samples and weigh more:

  | Zone class | Saturates confidence at | Min floor for a verdict | Per-sample weight |
  |---|---|---|---|
  | Workout (tempo/threshold/intervals) | 3 sessions (`calibration.target_samples.workout`) | 2 (`calibration.min_samples.workout`) | 2.0 (`calibration.sample_weight.workout`) |
  | Training run (recovery/base building) | 6 sessions (`calibration.target_samples.run`) | 4 (`calibration.min_samples.run`) | 1.0 (`calibration.sample_weight.run`) |

  Below the floor → that zone is `INSUFFICIENT_DATA`. Recovery deviation is additionally discounted as weak signal when computing the runner-level verdict.

## Confidence (0–100 → LOW / MED / HIGH), per zone

```
sample      = min(n_zone / target_samples[zoneClass], 1)
consistency = 1 - clamp(stdDev(relDev)_zone / calibration.std_ceiling)  // default ceiling 4%
recency     = decay on days since most recent eligible session in zone  // default half-life 10d
rawZone     = 100 * (0.45*sample + 0.35*consistency + 0.20*recency)
```

Runner-level confidence = sample-weighted blend of its zones' `rawZone` (weights from the table above), so a clean 3-sample tempo signal counts more than noisy recovery data.

**Anchor gate (the season-arc rule):** if the base is **coach-estimated** (not race-derived), cap confidence at MEDIUM. HIGH requires a **race-anchored** base. This gate is what keeps low-confidence early-season suggestions out of the insights feed in Phase 2.

---

## Resolved decisions

1. **Race-anchored detection — add a column** (not inference). Add to `runners_training_base_performance`:
   - `source VARCHAR` — `'RACE_DERIVED'` | `'COACH_ESTIMATE'` (default `'COACH_ESTIMATE'`)
   - `derived_from_meet_id INT NULL`

   Set `source = 'RACE_DERIVED'` wherever a base is built from a race result; manual creates stay `'COACH_ESTIMATE'`.

2. **Apply — confirm per runner.** Each suggestion shows current → suggested with a human delta; coach taps **Apply** to commit one runner at a time. Reuses existing write endpoints:
   - `BASE_OFF` → `TrainingBasePerformanceController` create/update base (stays `COACH_ESTIMATE` — a tuned estimate, not a race).
   - `ZONE_OFF` (training-run zones) → `createRunnersBaseTrainingPercentage`.
   - **Workout zones → advisory-only** (see limitation below).

### Known limitation

There is **no per-runner persistent workout-percent override** entity today. So for workout zones (tempo/threshold/intervals), the `Apply` write path has nowhere to write. Phase 1 scoping:

- Training-run zones (recovery, base building) → **full Apply**.
- Workout zones → **advisory-only** suggestions (e.g. "for Macy, 87.5% tempo plays ~3 s/mi slow — consider 89%").

Closing this requires the future work below.

---

## Phase 1 scope

1. **Migration:** add `source`, `derived_from_meet_id` to `runners_training_base_performance`; set `RACE_DERIVED` in the race-derived base path.
2. **`PaceCalibrationService`** — ingests **both** subsystems → eligibility filter → per-zone `relDev` aggregate → verdict (`BASE_OFF` / `ZONE_OFF` / `DIALED_IN` / `INSUFFICIENT_DATA`) → confidence with anchor gate.
3. **Response types** + read endpoint `GET xc/pace-calibration?season=&year=&team=&runnerId=(optional)` (mirrors `CoachInsightsController`):
   ```
   PaceCalibrationResponse(runners: List<RunnerPaceCalibration>)
   RunnerPaceCalibration(
     runner, currentBaseTime, baseSource,           // "race-anchored" | "estimated"
     confidence: Int, confidenceLabel,              // LOW/MED/HIGH
     verdict,                                        // BASE_OFF | ZONE_OFF | DIALED_IN | INSUFFICIENT_DATA
     suggestion: CalibrationSuggestion?,            // knob, currentValue, suggestedValue, deltaHuman, applicable: Bool
     zones: List<ZoneCalibration>                   // paceType, n, meanRelDev, expected vs observed, in/fast/slow counts
   )
   ```
4. **Config defaults** via `TrainingLoadConfigService.get("calibration.*", team, default)` — matches the existing insights pattern.
5. **iOS** — surfaced as its own **section within the coaches dashboard** (distinct from the alarm feed; feed promotion is Phase 2). Coach-only (gate on `subRole == "full_coach"`): roster list with confidence chip + one-line verdict (the at-a-glance "who's calibrated / who's not" view) → tap → per-zone breakdown (expected vs observed per zone, sample count, in/fast/slow counts) + suggestion card with **Apply** (disabled/advisory for workout zones).

---

## Future scope (documented, NOT in Phase 1)

### Per-runner, event-aware custom zones

Eventually each runner should have **custom tempo / threshold / etc. percentages**, because the right percentage depends on the athlete's event specialization and physiology — not just their 5k base.

> Example: an 800m specialist running a 5k has a **different 5k threshold** than a 3200m specialist, even at the same 5k time. Their speed-endurance profiles differ, so a single team-wide "90% = threshold" mapping is wrong for both.

Implications when we build this:

- Extend per-runner zone overrides to the **workout** subsystem (a per-runner persistent workout-percent override entity), which also unlocks **full Apply** for workout zones — removing the Phase 1 advisory-only limitation.
- Possibly event-aware default percentages (a runner's primary event informs starting zone %s before any calibration data exists).
- Calibration suggestions would then write per-runner workout overrides directly, closing the loop for tempo/threshold the same way recovery/base building works today.

### Insights-feed promotion (Phase 2)

Once bases are race-anchored, promote **persistent high-confidence** deviations into the existing `CoachInsightsService` feed as *athlete-state* signals (fatigue, fitness change) — gated on HIGH confidence so early-season noise never reaches the feed.

### Auto-recalibration

When a new race result lands, auto-update the runner's base and re-derive ranges, raising confidence and re-evaluating outstanding suggestions.
