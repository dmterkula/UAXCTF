#!/usr/bin/env python3
"""
XC Meet Splits/Passes/Skulls Updater

Takes the per-runner splits spreadsheet (mile splits, mile positions, finish
position) and updates the existing XC meet result rows (created earlier by
scrape_meet_results.py) with mile splits, passes, and skull beads earned.

XC only — uses /xc/meetResults/createForRunner, which upserts: since the
runner+meet row already exists from the raw results load, this just fills
in the additional fields rather than creating a duplicate.

This script deliberately does NOT touch time/place on existing records: it
looks up the runner's current time/place from the backend (millisecond
precision, from the original results load) and sends those back unchanged,
only overwriting mileOneSplit/mileTwoSplit/mileThreeSplit/passes*/skullsEarned.
If a runner has no existing record (e.g. they were unmatched during the raw
load), it falls back to the spreadsheet's finish time/final position and
flags it.

Skulls rule: a runner earns 1 skull bead for moving up in position from mile 1
to mile 2, and another for moving up from mile 2 to the finish. Max 2.

Input: a tab-separated file (paste straight out of Google Sheets/Excel),
first row = header, matching column names (case-insensitive, order-agnostic):
    Runner, Mile 1, Position Mile 1, Mile 1 to 2, Mile 2 Split,
    Mile 2 to Finish Pace, Position Mile 2, Finish Time, Average Pace,
    Final Position, [Passes 1 to 2, Passes 2 to Finish, Skulls Beads Earned]

The last three columns are computed from the position columns; if present
in the sheet they're used as a sanity check against the computed values and
any mismatch is flagged as a warning.

Split/time values are normalized: a leading all-zero hour segment is
stripped, e.g. "0:06:49" and "00:06:49" both become "06:49".

Usage:
    python scripts/update_meet_splits.py <tsv_file> <meet_name> <season> [--dry-run]
"""

import sys
import csv
import re
import requests
from difflib import get_close_matches

BASE_API_URL = "http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com"

COLUMN_ALIASES = {
    "runner": "runner",
    "mile 1": "mile1",
    "position mile 1": "posmile1",
    "mile 1 to 2": "mile2legtime",
    "mile 2 split": "mile2cumulative",
    "mile 2 to finish pace": "mile3legtime",
    "postion mile 2": "posmile2",
    "position mile 2": "posmile2",
    "finish time": "finish",
    "average pace": "avgpace",
    "final position": "finalpos",
    "passes 1 to 2": "passes12_given",
    "passes 2 to finish": "passes2f_given",
    "skulls beads earned": "skulls_given",
}


def normalize_name(name: str) -> str:
    if "," in name:
        parts = name.split(",", 1)
        name = parts[1].strip() + " " + parts[0].strip()
    name = re.sub(r"[^a-zA-Z ]", "", name).lower()
    return " ".join(sorted(name.split()))


def normalize_time(t: str) -> str:
    """Strip a leading all-zero hour segment, e.g. '0:06:49' / '00:06:49' -> '06:49'."""
    if not t:
        return t
    parts = t.split(":")
    if len(parts) == 3 and all(c == "0" for c in parts[0]):
        parts = parts[1:]
    return ":".join(parts)


def find_runner_id(scraped_name: str, roster: dict):
    key = normalize_name(scraped_name)
    if key in roster:
        return roster[key]
    matches = get_close_matches(key, roster.keys(), n=1, cutoff=0.82)
    if matches:
        print(f"  [FUZZY MATCH] '{scraped_name}' -> '{matches[0]}'")
        return roster[matches[0]]
    return None


def fetch_roster(base_url: str) -> dict:
    resp = requests.get(f"{base_url}/xc/allRunners/", timeout=10)
    resp.raise_for_status()
    return {normalize_name(r["name"]): r["id"] for r in resp.json()}


def fetch_existing_results(base_url: str, meet_name: str, season: str) -> dict:
    """Returns {runner_id: {"time": ..., "place": ...}} for the given meet/season."""
    resp = requests.get(
        f"{base_url}/xc/meetResults",
        params={"filter.meetName": meet_name, "filter.season": season, "page.size": 500},
        timeout=10
    )
    resp.raise_for_status()
    existing = {}
    for entry in resp.json():
        runner_id = entry["runner"]["id"]
        perf = entry["performance"]
        existing[runner_id] = {"time": perf["time"], "place": perf["place"]}
    return existing


def post_meet_result(base_url: str, payload: dict) -> None:
    resp = requests.post(
        f"{base_url}/xc/meetResults/createForRunner",
        json=payload,
        timeout=10
    )
    resp.raise_for_status()


def parse_rows(tsv_path: str) -> list:
    with open(tsv_path, newline="") as f:
        reader = csv.reader(f, delimiter="\t")
        rows = [r for r in reader if any(c.strip() for c in r)]

    if not rows:
        return []

    header = [h.strip().lower() for h in rows[0]]
    col_idx = {}
    for i, h in enumerate(header):
        key = COLUMN_ALIASES.get(h)
        if key:
            col_idx[key] = i

    required = ["runner", "posmile1", "posmile2", "finalpos", "finish", "mile1", "mile2legtime", "mile3legtime"]
    missing = [r for r in required if r not in col_idx]
    if missing:
        raise ValueError(f"Missing required column(s) in header: {missing}\nParsed header: {header}")

    def cell(row, key, default=""):
        idx = col_idx.get(key)
        if idx is None or idx >= len(row):
            return default
        return row[idx].strip()

    parsed = []
    for row in rows[1:]:
        runner = cell(row, "runner")
        if not runner:
            continue

        pos1 = int(cell(row, "posmile1"))
        pos2 = int(cell(row, "posmile2"))
        final_pos = int(cell(row, "finalpos"))

        passes12 = pos1 - pos2
        passes2f = pos2 - final_pos
        skulls = (1 if passes12 > 0 else 0) + (1 if passes2f > 0 else 0)

        given_passes12 = cell(row, "passes12_given")
        given_passes2f = cell(row, "passes2f_given")
        given_skulls = cell(row, "skulls_given")

        warnings = []
        if given_passes12 and int(given_passes12) != passes12:
            warnings.append(f"passes1to2 given={given_passes12} computed={passes12}")
        if given_passes2f and int(given_passes2f) != passes2f:
            warnings.append(f"passes2toFinish given={given_passes2f} computed={passes2f}")
        if given_skulls and int(given_skulls) != skulls:
            warnings.append(f"skulls given={given_skulls} computed={skulls}")

        parsed.append({
            "name": runner,
            "mileOneSplit": normalize_time(cell(row, "mile1")),
            "mileTwoSplit": normalize_time(cell(row, "mile2legtime")),
            "mileThreeSplit": normalize_time(cell(row, "mile3legtime")),
            "sheetTime": normalize_time(cell(row, "finish")),
            "sheetPlace": final_pos,
            "passesSecondMile": passes12,
            "passesLastMile": passes2f,
            "skullsEarned": skulls,
            "warnings": warnings,
        })

    return parsed


def main(tsv_path: str, meet_name: str, season: str, dry_run: bool):
    print("=" * 60)
    print(f"Meet:    {meet_name}")
    print(f"Season:  {season}")
    print(f"Mode:    {'DRY RUN' if dry_run else 'LIVE'}")
    print("=" * 60)

    print("\nFetching roster from backend ...")
    roster = fetch_roster(BASE_API_URL)
    print(f"Loaded {len(roster)} runners")

    print("Fetching existing meet results (to preserve time/place) ...")
    existing_results = fetch_existing_results(BASE_API_URL, meet_name, season)
    print(f"Found {len(existing_results)} existing result(s) for this meet\n")

    rows = parse_rows(tsv_path)
    print(f"Parsed {len(rows)} row(s) from {tsv_path}\n")

    unmatched = []
    updated = []

    for r in rows:
        runner_id = find_runner_id(r["name"], roster)

        print(f"  {r['name']:<25} "
              f"passes1to2={r['passesSecondMile']:<3} passes2toFinish={r['passesLastMile']:<3} skulls={r['skullsEarned']} "
              f"splits=[{r['mileOneSplit']}, {r['mileTwoSplit']}, {r['mileThreeSplit']}]")
        for w in r["warnings"]:
            print(f"    [WARNING] {w}")

        if runner_id is None:
            print(f"    [UNMATCHED] could not find '{r['name']}' in roster")
            unmatched.append(r)
            continue

        existing = existing_results.get(runner_id)
        if existing:
            time = existing["time"]
            place = existing["place"]
            if place != r["sheetPlace"]:
                print(f"    [WARNING] sheet final position ({r['sheetPlace']}) != stored place ({place}); keeping stored place")
        else:
            print(f"    [WARNING] no existing result found for this runner at this meet; using sheet time/place")
            time = r["sheetTime"]
            place = r["sheetPlace"]

        payload = {
            "season": season,
            "runnerId": runner_id,
            "meetName": meet_name,
            "time": time,
            "place": place,
            "passesLastMile": r["passesLastMile"],
            "passesSecondMile": r["passesSecondMile"],
            "skullsEarned": r["skullsEarned"],
            "mileOneSplit": r["mileOneSplit"],
            "mileTwoSplit": r["mileTwoSplit"],
            "mileThreeSplit": r["mileThreeSplit"],
        }

        if dry_run:
            print(f"    [DRY RUN] would upsert (time={time}, place={place})")
        else:
            post_meet_result(BASE_API_URL, payload)
            print(f"    [UPDATED]")
            updated.append(r)

    print(f"\n{'='*60}")
    if dry_run:
        print(f"Dry run complete. Would update {len(rows) - len(unmatched)} result(s).")
    else:
        print(f"Done. Updated {len(updated)} result(s).")

    if unmatched:
        print(f"\n[!] {len(unmatched)} runner(s) could not be matched to the roster:")
        for r in unmatched:
            print(f"   '{r['name']}'")


if __name__ == "__main__":
    args = sys.argv[1:]
    dry_run = "--dry-run" in args
    args = [a for a in args if not a.startswith("--")]

    if len(args) < 3:
        print("Usage: python scripts/update_meet_splits.py <tsv_file> <meet_name> <season> [--dry-run]")
        sys.exit(1)

    main(tsv_path=args[0], meet_name=args[1], season=args[2], dry_run=dry_run)