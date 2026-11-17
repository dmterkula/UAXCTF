#!/usr/bin/env python3
"""
Race Roster Results Loader
Pulls Ursuline Academy results for one race from results.raceroster.com (via its JSON API)
and loads them into the UAXCTF backend. Safe to re-run: the backend upserts per runner/meet.

Usage:
    python scripts/scrape_raceroster.py <race_url> <meet_name> <season> [--dry-run]

Example:
    python scripts/scrape_raceroster.py \
        "https://results.raceroster.com/v3/events/ptjupnjkwxq7pycc/race/382037" \
        "New Richmond" 2026 --dry-run
"""

import re
import sys
import requests

from scrape_meet_results import BASE_API_URL, fetch_roster, find_runner_id, post_meet_result

RR_API = "https://results.raceroster.com/v2/api"
TEAM_PATTERN = re.compile(r"ursuline", re.IGNORECASE)
HEADERS = {"User-Agent": "Mozilla/5.0"}


def parse_race_url(url: str) -> tuple[str, int]:
    m = re.search(r"/events/([a-z0-9]+)/race/(\d+)", url)
    if not m:
        sys.exit(f"Not a Race Roster race URL: {url}")
    return m.group(1), int(m.group(2))


def normalize_time(t: str) -> str:
    """'00:18:45.3' -> '18:45.3'; leaves 'M:SS' alone."""
    t = t.strip()
    while re.match(r"^0+:\d{1,2}:", t):
        t = t.split(":", 1)[1]
    return t.lstrip("0") if re.match(r"^0\d:", t) else t


def fetch_race(event_code: str, race_id: int) -> tuple[str, list]:
    event = requests.get(f"{RR_API}/events/{event_code}", headers=HEADERS, timeout=15).json()["data"]["event"]
    race_name = next((s["name"] for s in event["subEvents"] if s["id"] == race_id), str(race_id))
    rows, start = [], 0
    while True:
        resp = requests.get(
            f"{RR_API}/result-events/{event['resultEventId']}/sub-events/{race_id}/results",
            params={"start": start, "limit": 500}, headers=HEADERS, timeout=15,
        )
        resp.raise_for_status()
        body = resp.json()
        rows.extend(body["data"])
        start += len(body["data"])
        if not body["data"] or start >= body["meta"]["totalResults"]:
            break
    return race_name, rows


def main(race_url: str, meet_name: str, season: str, dry_run: bool):
    event_code, race_id = parse_race_url(race_url)
    race_name, rows = fetch_race(event_code, race_id)
    ours = [r for r in rows if TEAM_PATTERN.search(r.get("teamName") or "")]
    timed = [r for r in ours if r.get("gunTime") or r.get("chipTime")]
    print(f"{race_name}: {len(rows)} total rows, {len(ours)} Ursuline, {len(timed)} with a time "
          f"({'DRY RUN' if dry_run else 'LIVE'})")
    if not timed:
        return

    roster = fetch_roster(BASE_API_URL)
    created, unmatched = 0, []
    for r in timed:
        runner_id = find_runner_id(r["name"], roster)
        if runner_id is None:
            unmatched.append(r["name"])
            continue
        time_val = normalize_time(r.get("chipTime") or r["gunTime"])
        payload = {
            "season": season, "runnerId": runner_id, "meetName": meet_name,
            "time": time_val, "place": r["overallPlace"],
            "passesLastMile": 0, "passesSecondMile": 0, "skullsEarned": 0,
            "mileOneSplit": "", "mileTwoSplit": "", "mileThreeSplit": "",
            "divisionSequence": 1, "divisionLabel": race_name,
        }
        if dry_run:
            print(f"  [DRY RUN] {r['name']:<28} place={r['overallPlace']:<4} time={time_val}")
            continue
        try:
            post_meet_result(BASE_API_URL, payload)
            created += 1
            print(f"  [CREATED] {r['name']:<28} place={r['overallPlace']:<4} time={time_val}")
        except requests.HTTPError as e:
            print(f"  [ERROR]   {r['name']} — {e}")

    if not dry_run:
        print(f"Done. Created/updated {created} result(s).")
    for name in unmatched:
        print(f"  [UNMATCHED] {name}")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if len(args) != 3:
        sys.exit(__doc__)
    main(args[0], args[1], args[2], dry_run="--dry-run" in sys.argv)
