---
name: xc-meet-recap
description: Post a meet's mile-split/passes/skulls data from a spreadsheet and generate the meet recap email with tryout-beat and strong-early-split callouts. Use when the user gives a spreadsheet of XC mile splits for a meet and asks to post results and/or build the meet summary email.
---

Base API: `http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com`. All scripts need `requests`; if the system Python lacks it, make a venv (`python3 -m venv <scratchpad>/venv && <venv>/bin/python3 -m pip install requests`) and call `<venv>/bin/python3` directly rather than `source .../activate` (activate scripts are unreliable across tool calls in this environment).

## Step 1 — Post mile splits, passes, and skulls

Spreadsheet columns (tab-separated, header case-insensitive): `Runner, Mile 1, Position Mile 1, Mile 1 to 2, Mile 2 Split, Mile 2 to Finish Pace, Postion Mile 2, Finish Time, Average Pace, Final Position, Passes 1 to 2, Passes 2 to Finish, Skulls Beads Earned`.

Save the pasted data as a `.tsv` file, then:
```
python3 scripts/update_meet_splits.py <file.tsv> "<Meet Name>" <season> --dry-run
```
Check the dry run: every runner should match the roster with no `[UNMATCHED]`. `[WARNING] skulls given=X computed=Y` mismatches are expected sometimes (the script computes skulls/passes from the position columns, not the sheet's own values) — ask the user whether to trust the computed value or the sheet's value before proceeding live; computed has been the right call every time so far. Then rerun without `--dry-run`.

If the meet is brand new (no scrape has ever run for it — e.g. a hand-timed intrasquad meet), the meet row must already exist server-side (ask the user for the meet name + id if unsure — don't try to create one via the script, it doesn't create meets). The script gracefully falls back to the sheet's own finish time/place when there's no existing result row to preserve, which is exactly what you want for a hand-timed meet with no FAT data.

## Step 2 — Generate the recap email

```
python3 scripts/meet_summary.py "<Meet Name>" <season> --html
```
Writes `meet_summary_<Meet>_<season>.html` in repo root. Known bug (fixed once already, watch for regressions): `fmt_time()` used to strip a trailing `0` off whole times like `23:10` → `23:1` via a bad regex (`\.?0+$` with optional dot). Correct version only strips trailing zeros after an actual decimal point:
```python
def fmt_time(t: str) -> str:
    t = t.strip()
    if "." in t:
        t = t.rstrip("0").rstrip(".")
    return t
```
Sanity-check any time ending in a bare `0` (e.g. `23:10`, `21:40`) in the generated HTML before sending.

If the user supplies their own hand-edited copy for the per-runner blurbs, preserve the `<strong>` tags around the runner name, place, and time (and `PR`/`SB` if present) — matching the original template's bold styling — don't just paste plain text over it.

## Step 3 — "Nice things to say" callouts (tryout beat + strong splits)

**Beat their tryout time:** compare this meet's finish time against the runner's **season-specific** tryout time, not their all-time-best. Use:
```
GET /xc/timeTrialResults?filter.season=<season>&filter.scaleTo5k=true
```
(NOT `/xc/leaderboard/tryouts` — that endpoint returns each runner's all-time-best tryout across all years, which understates who actually improved this season.) `scaleTo5k=true` scales the raw time trial (a 3.0-mile / 4827m course — confirmed in `TimeUtilities.kt`'s `scaleTimeTo5k`) linearly to a 5k-equivalent, which is the correct distance adjustment for this team's actual time-trial course. Diff each runner's meet time against this adjusted tryout time in seconds; report the exact margin (e.g. "beat their tryout time by 1:52"), not just a boolean.

**Strong early-race splits (mile 1 + mile 2) vs. season norm:** pull two endpoints:
```
GET /xc/meetMileSplits/comparisonToPaceAtMeet?filter.meet=<Meet Name>&filter.season=<season>&sort.split=1
GET /xc/meetMileSplits/comparisonToPaceAtMeetsForAllRunners?filter.season=<season>&sort.split=1
```
Both are season-wide already (no need to loop meet-by-meet) and both cover every meet result on file for that season/meet — don't assume it's diluted by "early season, few meets," verify meet count if in doubt via `/xc/meetMileSplits/comparisonToPaceAtMeetForRunner?filter.runner=<name>&filter.season=<season>` (`meetSplits` array length = number of meets counted).

For each runner in both responses, for mile 1 and mile 2:
1. `comparison_pace_sec = today_split_seconds / today_percentOfComparison` (back out their PR-equivalent pace for that mile).
2. `typical_time_sec = comparison_pace_sec * season_average_percentOfComparison`.
3. `diff = typical_time_sec - today_split_seconds` (positive = ran faster than typical today).

Sum mile-1 + mile-2 diffs for a single "seconds ahead of usual early-race pace through two miles" number — this reads as concrete and understandable to high-school athletes, unlike raw percentages. Only call out runners with a clearly positive combined diff (a few seconds either way is noise, not a real signal).

## Phrasing conventions for this recap doc
- Default to "they/their" for the auto-generated inline clauses, even in a doc where the user's own hand-written sentences use "she/her" for these specific athletes — don't invent a pronoun choice for a person you don't have explicit confirmation on, even when the surrounding sentence already picked one.
- Keep each callout as one added sentence at the end of that runner's existing paragraph, not a separate summary block — the user wants it inline per runner, not appended in bulk at the bottom.
