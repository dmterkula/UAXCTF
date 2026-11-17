---
name: create-track-workouts
description: Create or edit UA XC/Track training events (workouts, training runs, cross-training, individual-athlete plans) through the backend API so paces generate and the iPhone app doesn't crash. Use when asked to build events from a coach's calendar, make an individual runner's week, fix a workout's paces, or tag events to track groups.
---

# Creating track/XC workouts safely

## Hard rules
- **NEVER send notifications from automated creation.** The app only pushes when the coach form's "Notification" toggle is on
  (default OFF) and pushes from the client, not the API. Scripts must only call create/update/visibility/groups endpoints.
  Use `workout_contract.call()`; it refuses every other path. Never drive the form in a simulator to save real events.
- **Build components with `workout_contract.component()` and run `workout_contract.py <dry-run.json>` before posting.**
  The contract encodes what the app's own form always sends, so payloads match what the UI could produce.
  Lint what is already live with `python3 workout_contract.py --server <start> <end>` (expect 0 errors).

The API accepts data the iOS app cannot render. Several rules below exist because a request returned 200
and the app then crashed or showed no paces. Always finish with the verification steps.

API base: `https://7uyfdpbnyj.execute-api.us-east-2.amazonaws.com/` (no auth header).

## Endpoints
- Workout: `POST xc/workout/create`, `PUT xc/workout/update?workoutUUID=` (replaces components), `GET xc/workout/get?startDate=&endDate=`
- Training run: `POST xc/training-run/create` (icon 👟). Cross-training: `POST cross-training-activity/create`.
- Plan/paces: `GET xc/workout/plan?uuid=`. Runner result: `GET xc/workout/runnerResult?workoutUuid=&runnerId=`
- Visibility (one athlete only): `PUT api/events/{uuid}/visibility` body `{"runnerIds":[id]}`. Empty = everyone.
- Group tag: `PUT api/events/{uuid}/groups` body `{"groups":["distance"|"mid-distance"|"400"]}`. Empty = all groups.
- Every event: `team:"UA"`, `season:"track"|"xc"`, new `uuid`. Icons: 📌 workout, 👟 training run, ✨ general cross-training.
- Name the group in the title ("Distance: ...", "400 Group: ...", "Macy: ..."); one event per group, not per color range.

## Component rules (ComponentCreationElement)
1. Every component needs its own `uuid` (missing = 400).
2. Keep component `description` <= ~200 chars (longer = 500 and the workout is half-created; delete it and retry).
3. Never leave a pace-bearing session as `descriptionOnly`. Use `type` Interval/Tempo/Progression with
   `pace:"Base Training Pace"`, `targetEvent` (400/800/1600/3200/5000), `percent`, `targetDistance`, `targetCount`,
   `ratio:1.0`, `sets:1`, `setRecovery:"00:00"`, `targetPaceAdjustment:"0:00.0"`.
   - Timed reps: set `duration:"mm:ss"` (distance is then ignored for the target).
   - 400 group: targetEvent 400, percent of 400m base (accels 100, tempo 100s 70, 200s 70).
   - Distance/threshold work: targetEvent 5000 (or 3200) with the coach's percent.
   - Warm-up/cool-down stay in the description; athletes log them separately.
4. **Recovery (crashes older app builds if wrong).** The form ALWAYS sends `recoveryType` (default Standing), `recoveryDistance`
   (Int, default 0, never null), and `duration`/`recovery`/`setRecovery` as "mm:ss" ("00:00" when unused). Omitting `recoveryDistance`
   on an Active recovery crashed the plan screen (force-unwrap). Timed jog/rest: `Standing` + `recovery:"mm:ss"` + `recoveryDistance:0`.
   Distance jog: `Active` + `recoveryDistance` 100..1600. (Active + distance 0 = the form's "time based" toggle; valid but the plan
   screen shows "Recovery Jog: 0".)
5. Hill / feel-based sessions with no meaningful flat pace stay `descriptionOnly`; say so to the user.

## Pace data
- Paces come from `runners_training_base_performance` (event, season, year, seconds, source COACH_ESTIMATE/SEASON_COPY).
  `<targetEvent>m` base at the workout date; Nov/Dec track workouts use NEXT year's base.
- Before creating, list every targeted runner who lacks a base for that event/year and report them. Do not silently skip.
- Individual athlete bases: confirm with the coach (e.g. 5k track base, 3200). Fix the DB row if wrong (check distance event and fraction_of_miles).

## Form contract (from WorkoutComponentFormViewModel / WorkoutFormView)
type in Interval|Tempo|Progression (descriptionOnly is API-only legacy); pace in SB|SB Avg|PR|Goal|Base Training Pace;
targetEvent in 400|800|1600|3200|5000; percent 50..120 (default 100); form requires non-zero targetDistance (even for timed reps),
a description, and a targetEvent; ratio null only when 0; sets Int. Workout also sends `startTime` "HH:mm:ss" and optional `venueId`.

## Verification (do not report done until all pass)
1. `xc/workout/get` for the date range: confirm components, `assignedGroups`, `assignedRunnerIds`.
2. `xc/workout/plan?uuid=`: every targeted runner has a `targetedPace` entry; spot-check 2 runners by hand.
3. `python3 workout_contract.py --server <start> <end>` reports 0 errors (catches null recoveryDistance, null recovery fields, etc.).
4. Individual plans: confirm other groups' filters don't show them (untagged + assigned runner = hidden under a filter).
5. Open it in the app (simulator). `UAXCUITests/ReplyQueueScreenshotTests.swift` has `RunnerLogWorkoutReproTests`
   (env `UAXC_RUNNER_USER` / `UAXC_RUNNER_PASS`, prefixed `TEST_RUNNER_` for xcodebuild). It signs in, opens the workout, screenshots it.
   A crash shows as "Target Application does not exist"; the .ips is in `~/Library/Logs/DiagnosticReports/`.

## Safety when signing in as an athlete
- Login overwrites `app_users.device_id` AND `runners.device_id` (kills her push notifications). Record both first and restore
  after with a SQL file (the Mig.java helper's `sql` mode is SELECT-only; use file mode for UPDATE).
- Don't save a test log against a real athlete without asking: it awards points and shows on her record.
- Always dry-run (JSON file) and show the user before bulk-creating; deploys and DB changes need their go-ahead.
