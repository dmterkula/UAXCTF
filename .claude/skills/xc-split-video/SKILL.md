---
name: xc-split-video
description: Read a coach's mile-mark video from an XC meet and output each Ursuline (UA) runner's split and place at that mark, optionally checked against the backend. Use when the user gives a video (plus optional bib-sheet / team photos) taken at a mile split and asks for UA runners' splits, places, or positions.
---

# XC mile-split video → UA splits and places

The coach films each mile mark holding a Garmin watch (race clock) in frame. Each runner's split is the
watch reading as they pass the camera; their place is how many runners (all teams) have passed by then.

## Inputs to ask for / look for
- **Video** (`.MOV`). iPhone videos keep their *recording* date as mtime, so "latest file in ~/Downloads" by
  `ls -t` can miss it — sort by when it was added: `ls -ltc ~/Downloads | grep -i mov`.
- **Which meet, race/division and mile** this is. Confirm from the watch: mile-1 times are roughly 6–10 min,
  mile-2 roughly 12–20 min. Northmont 2026's "mile 2" video was actually Open-race mile 1 — check before
  reporting, and say so if it doesn't match what the user said.
- **Bib sheet photo** (bib → name), if given. Without it, names come from backend order (see Step 5).
- **Race-day team photo** (each girl with bib, shoes, hair visible), if given — best way to identify
  runners whose bib is unreadable. Treat any athlete photos as private: keep them local, never upload.

## Setup (once per session, in the scratchpad)
Tools here: no ffmpeg / OpenCV. Use AVFoundation via Swift plus Pillow in a venv.
```
S=<scratchpad>; K=~/Desktop/projects/UAXCTF/.claude/skills/xc-split-video
swiftc -O $K/extract_frames.swift -o $S/extract_frames
python3 -m venv $S/venv && $S/venv/bin/pip -q install pillow requests
```
Run the Python scripts with `$S/venv/bin/python -I`. Put video frames in their own folders under `$S`.

## Step 1 — Overview and watch calibration
1. `extract_frames <video> $S/ov 5 0 99999 480` then `contact_sheet.py $S/ov_sheet 7 14 0 1 0 1 $S/ov/*.jpg`
   and look at the sheets: orientation, where runners pass, where the watch is.
2. Read the watch at several video times (full-res crops of the watch area). Confirm
   `watch = video_t + offset` holds across the clip (it did at GGCL and Northmont: one-for-one, no drift).
   If the watch is out of frame at the start, calibrate from later readings.
3. Pin the sub-second offset by extracting 0.1 s frames around one watch tick if the digits are legible;
   otherwise accept ±1 s (GGCL: 9 of 14 exact, rest within 1 s).

## Step 2 — Find every UA runner
- UA uniform: **dark green singlet, "UA" with lion logo on the chest, gold/black diagonal side stripes.**
- **Look-alikes:** Mason wears dark green too ("MASON" across chest, white/light side panels) and at
  Northmont its bibs were in the **same 21xx range** as UA. Bib range alone does not identify UA — confirm
  logo/stripes. Seton (light grey "SETON") and others are easy to rule out.
- Pass 1: 1 s frames at ~720 px, contact sheets of the middle band (`0.15 0.65 0 1`), 18–30 per sheet.
  Note every timestamp a green singlet approaches.
- Pass 2: full-res (1920) 0.25–0.5 s frames around each candidate; crop toward the runner and read the bib.
  Pick the frame where the runner is closest to the camera — that is the passing moment for the split.

## Step 3 — Count places
Count **every** runner (all teams) who passes, in order, at 0.5 s resolution. Dense packs (10+ runners
within a few seconds) must be checked at full resolution frame by frame. A runner's place = count of
runners through the camera up to and including her.
- Places are **within the video**. If recording started after the leader, say so and ask how many passed
  earlier — never silently assume the first runner on video is 1st.
- Say whether the count includes all divisions (JV/varsity often run together, e.g. GGCL).

## Step 4 — Output
A table per runner: bib, name (if known), split (`watch time`), place, and video timestamp. Save a CSV in
the user's working folder. Flag explicitly:
- runners identified by singlet only (bib unreadable) — list them separately;
- bib reads that conflict (same number read on another team's runner);
- uncertain splits (watch blurred / out of frame at the passing moment).

## Step 5 — Check against the backend (when the meet is already loaded)
```
$S/venv/bin/python -I $K/backend_splits.py "<Meet Name>" <season> --mile 2   # or --mile 1
```
- Backend cumulative split for mile 2 = `mileOneSplit + mileTwoSplit`.
- Position at a mile, from passes: `pos_mile2 = place + passesLastMile`,
  `pos_mile1 = place + passesLastMile + passesSecondMile`. `place` is the **division** place while passes
  count everyone on course, so these positions are approximate when divisions share a race.
- Name matching: the order of UA runners on video vs. sorted backend splits is the strongest signal; it
  matched exactly at GGCL. Use it, plus the bib sheet, to attach names.
- Report misses as counts by group (backend runners in a time window vs. UA singlets seen there).

## Known results (for calibration)
- **GGCL 2026, mile 2 (IMG_6817)**: 14/14 UA runners found, all bibs (6xx) read; splits within 1 s of backend.
- **Northmont 2026, Open mile 1 (IMG_2209)**: ~21 of 22 found; splits within ~1–3 s; 5 bib reads
  uncertain, 3 unreadable, 2 possibly Mason. Crowded races + Mason look-alikes are the main failure mode.

## Tips for the coach (repeat if relevant)
Start recording a few seconds before the leader arrives; keep the watch in frame and in focus; photograph
the bib sheet and the team at the start line on race day.
