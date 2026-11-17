---
name: xc-meet-info
description: Build the weekly UA XC pre-meet info sheet (PDF + editable Word copy) and the team email draft from an Athletic.net meet page or host PDF. Use when the user gives a meet URL/PDF and asks for the meet info sheet, lineups, or the email to the team.
---

# XC pre-meet info sheet

Tools live in `scripts/meet_info/` (run with `scripts/venv/bin/python`):
- `rank_athletes.py` – active UA XC athletes ranked by 2026 season best (UAXCTF API).
- `build_meet_info.py specs/<meet>.json` – renders `out/<meet>.pdf` + `.docx`. Arrival/warm-up/line-up
  times are computed from race time and offsets (defaults: arrive 85, warm-up 60, line-up 15 min before).
- `specs/cedarville_2026_example.json` – reference spec; copy it for a new meet.

## Workflow
1. **Get the meet info.** Athletic.net returns 403 to curl/WebFetch, so open the meet's `/info` page in the
   built-in browser (`mcp__Claude_Browser__navigate`), then read the "Download Meet Files" link with
   `javascript_tool` (`document.querySelectorAll('a')`). The PDF is a public
   `storage.googleapis.com/anet_user_files/...` URL; ask before downloading, then `curl` it and Read it.
   If the page is blocked, ask the user to drop the PDF in ~/Downloads.
2. **Extract**: venue, address, date, each UA-relevant race + start time, awards, parking/spikes/results
   site/course-map notes. Flag anything ambiguous instead of guessing.
3. **Ask the user** (one AskUserQuestion round): which races/divisions UA is running, top-N per race,
   arrival offset (default 85, or 90 if they want extra time), any athletes not running or pending (`*`).
4. **Seed lineups** with `rank_athletes.py`: fill races by rank per the user's split, show the proposal,
   let the user adjust. Names with a trailing `*` render as "pending clearance/coaches decision".
5. **Write the spec** in `scripts/meet_info/specs/<slug>.json` and run the builder. Open the PDF and
   sanity-check page 1 and the times. Optional `course_map` = path to a PNG/JPG (screenshot the host PDF page).
6. **Email draft**: short body (meet, date, venue+address, race time, arrive/warm-up times per race, what
   to bring, link to the Athletic.net meet page) and tell the user which files to attach. Never send;
   there is no Gmail connector unless the user adds one, and sends need explicit approval.

## Notes
- Spec fields: `venue, address, address_note, date, races[{name,time,athletes,expected_size,arrive_min…}],
  not_running, awards_time, awards[], notes[], results_site, bring[], team_cheer, course_map`.
- Multiple races (e.g. Troy White/Gray/Scarlet) = multiple entries in `races`; each gets its own schedule block.
- `scripts/meet_info/out/` is generated output; don't commit it.
