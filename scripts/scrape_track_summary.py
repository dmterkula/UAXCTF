#!/usr/bin/env python3
"""
Track Meet Summary Scraper
Scrapes Ursuline Academy results from milesplit.live or finishtiming.trackscoreboard.com
and generates a formatted HTML summary you can copy-paste into an email.

Supports two timing providers:
  - milesplit:    Uses Playwright to scrape the Angular SPA at milesplit.live
  - finishtiming: Uses direct Firebase REST API (no browser needed)

Setup (one time):
    cd /path/to/UAXCTF
    python3 -m venv scripts/venv
    source scripts/venv/bin/activate
    pip install playwright requests
    python -m playwright install chromium

Each session:
    source scripts/venv/bin/activate

Usage:
    python scripts/scrape_track_summary.py --source <milesplit|finishtiming> <meet_url> <meet_name> [--debug]

Examples:
    python scripts/scrape_track_summary.py --source milesplit \
        "https://milesplit.live/meets/718150/schedule" "Badin Invitational"

    python scripts/scrape_track_summary.py --source finishtiming \
        "https://finishtiming.trackscoreboard.com/meets/645654/events" "Madeira Invitational"

Output:
    Writes track_summary_<meet_name>.html to the current directory.
    Open in browser → Cmd+A → Cmd+C → paste into email.
"""

import sys
import re
import os
import csv
import json
import time
import uuid as uuid_mod
import urllib.request
from difflib import get_close_matches

TEAM_NAME = "Ursuline Academy"
BASE_API_URL = "http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com"

# Distance events eligible for auto-posting (normalized names)
POSTABLE_EVENTS = {
    "400m", "800m", "1600m", "3200m",
    "mile", "2 mile", "3000m Steeplechase",
    "3000m", "1500m", "600m",
}
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PR_CSV = os.path.join(SCRIPT_DIR, "athlete_prs.csv")
SB_CSV = os.path.join(SCRIPT_DIR, "season_bests.csv")


# ── Shared Helpers ────────────────────────────────────────────────────────────

def ordinal(n: int) -> str:
    if 11 <= (n % 100) <= 13:
        return "th"
    return {1: "st", 2: "nd", 3: "rd"}.get(n % 10, "th")


def place_str(p: int) -> str:
    return f"{p}{ordinal(p)}"


def _is_ua_team(text: str) -> bool:
    return TEAM_NAME in text


def _squad(team: str) -> str:
    """Return 'B' if the team string indicates the B squad, else 'A'."""
    if re.search(r'Academy\s+B\b', team):
        return "B"
    # finishtiming uses relay_team_letter
    if re.search(r'\bB\s*$', team):
        return "B"
    return "A"


def _extract_meet_id(meet_url: str) -> str:
    """Extract the numeric meet ID from a URL."""
    m = re.search(r"/meets/(\d+)", meet_url)
    if not m:
        raise ValueError(f"Cannot extract meet ID from URL: {meet_url}")
    return m.group(1)


# ── Event Name Normalization ──────────────────────────────────────────────────

def _normalize_event_name(event_name: str) -> str:
    """
    Normalize a meet event name to the canonical form used in athlete_prs.csv.
    E.g. "Girls 800 meter Run" -> "800m"
         "Womens 4x800 Relay" -> "4x800m Relay"
         "Girls Shot Put" -> "Shot Put"
         "Girls 100m Hurdles GGCL" -> "100m Hurdles"
         "400m Dash High School" -> "400m"
    """
    # Strip gender prefix
    n = re.sub(r'^(?:Girls|Womens|Women\'s|Boys|Mens|Men\'s)\s+', '', event_name, flags=re.IGNORECASE).strip()
    # Strip level/round/meet-specific suffixes
    n = re.sub(r'\s+(?:Varsity|JV|Junior\s+Varsity|Freshman|Open|Prelims|Finals|Prelim|Final)$', '', n, flags=re.IGNORECASE).strip()

    # Relay events: "4x800 Relay", "4x800mR", "4x800m Relay"
    m = re.match(r'(4x\d+)\s*(?:m(?:eter)?\s*)?\s*(?:Relay|R)\b', n, re.IGNORECASE)
    if m:
        return f"{m.group(1)}m Relay"

    # Running events: "800 meter Run" -> "800m", "100 meter Hurdles" -> "100m Hurdles"
    # Only match the core descriptor (Run/Dash/Hurdles/H); ignore trailing meet names
    m = re.match(r'(\d+)\s*(?:meter|meters|m)?\s*(.*)', n, re.IGNORECASE)
    if m:
        dist = m.group(1)
        suffix = m.group(2).strip()
        # Extract just the first word of the suffix to identify the event type
        first_word = suffix.split()[0].lower() if suffix else ''
        if first_word in ('run', 'dash', ''):
            return f"{dist}m"
        if first_word in ('hurdles', 'h'):
            return f"{dist}m Hurdles"
        if first_word == 'steeplechase':
            return f"{dist}m Steeplechase"
        # If first word looks like a meet name (not a known event descriptor), just use distance
        if first_word and first_word not in ('mile', 'relay'):
            return f"{dist}m"
        if suffix:
            return f"{dist}m {suffix}"
        return f"{dist}m"

    # Normalize field event names — match on the first words, ignoring trailing meet names
    field_aliases = {
        "discus throw": "Discus",
        "discus": "Discus",
        "shot put": "Shot Put",
        "long jump": "Long Jump",
        "high jump": "High Jump",
        "pole vault": "Pole Vault",
        "triple jump": "Triple Jump",
        "javelin throw": "Javelin",
        "javelin": "Javelin",
    }
    n_lower = n.lower()
    # Try matching longest alias first (e.g. "discus throw" before "discus")
    for alias in sorted(field_aliases, key=len, reverse=True):
        if n_lower == alias or n_lower.startswith(alias + " "):
            return field_aliases[alias]

    return n


# ── Mark Parsing & Comparison ────────────────────────────────────────────────

def _parse_mark_to_seconds(mark: str) -> float | None:
    """
    Parse a time mark to seconds for comparison.
    Handles: "10:42.64" -> 642.64, "51.39" -> 51.39, "2:43.25" -> 163.25
    Returns None if unparseable or not a time.
    """
    if not mark or mark in ("--", "DNS", "DNF", "NH", "SCR", "FS"):
        return None
    mark = mark.strip()
    # Strip qualifying/advancement suffixes (e.g. "17.72q", "1:01.61Q")
    mark = re.sub(r'[qQ]$', '', mark)
    # MM:SS.ss
    m = re.match(r'^(\d+):(\d+(?:\.\d+)?)$', mark)
    if m:
        return int(m.group(1)) * 60 + float(m.group(2))
    # SS.ss
    m = re.match(r'^(\d+\.\d+)$', mark)
    if m:
        return float(m.group(1))
    return None


UNICODE_FRACTIONS = {
    '¼': 0.25, '½': 0.5, '¾': 0.75,
    '⅛': 0.125, '⅜': 0.375, '⅝': 0.625, '⅞': 0.875,
    '⅓': 1/3, '⅔': 2/3,
}


def _parse_field_mark_to_inches(mark: str) -> float | None:
    """
    Parse a field mark to inches for comparison.
    Handles: "15-5.50" -> 185.5, "4-6" -> 54, "67-10" -> 814
             "94' 0\"" -> 1128, "13' 3\"" -> 159
             "24' 10½\"" -> 298.5, "14' 8¾\"" -> 176.75
    Returns None if unparseable.
    """
    if not mark:
        return None
    mark = mark.strip()
    # feet-inches.fraction format: "15-5.50", "4-6"
    m = re.match(r'^(\d+)-(\d+(?:\.\d+)?)$', mark)
    if m:
        return int(m.group(1)) * 12 + float(m.group(2))
    # feet' inches" format with optional unicode fraction: "94' 0\"", "24' 10½\""
    m = re.match(r"^(\d+)'\s*(\d+)([¼½¾⅛⅜⅝⅞⅓⅔])?\"?$", mark)
    if m:
        inches = int(m.group(2))
        if m.group(3):
            inches += UNICODE_FRACTIONS.get(m.group(3), 0)
        return int(m.group(1)) * 12 + inches
    # feet-inches.fraction with .00 suffix: "70-11.00"
    m = re.match(r'^(\d+)-(\d+)\.(\d+)$', mark)
    if m:
        return int(m.group(1)) * 12 + int(m.group(2)) + int(m.group(3)) / (10 ** len(m.group(3)))
    return None


def _is_field_event(event_name: str) -> bool:
    n = event_name.lower()
    return any(x in n for x in ["jump", "vault", "throw", "put", "discus", "javelin"])


def _is_better(new_mark: str, old_mark: str, event_name: str) -> bool:
    """Return True if new_mark is better than old_mark for the given event."""
    if _is_field_event(event_name):
        new_val = _parse_field_mark_to_inches(new_mark)
        old_val = _parse_field_mark_to_inches(old_mark)
        if new_val is not None and old_val is not None:
            return new_val > old_val  # bigger is better for field
    else:
        new_val = _parse_mark_to_seconds(new_mark)
        old_val = _parse_mark_to_seconds(old_mark)
        if new_val is not None and old_val is not None:
            return new_val < old_val  # smaller is better for running
    return False


# ── PR / SB Loading & Tracking ───────────────────────────────────────────────

def _load_prs(pr_csv: str) -> dict:
    """
    Load PRs from CSV. Returns dict keyed by (athlete, event) -> pr_mark.
    Loads all season types (outdoor, indoor, cc) and keeps the best mark per event.
    """
    prs = {}
    if not os.path.exists(pr_csv):
        return prs
    with open(pr_csv, newline="") as f:
        for row in csv.DictReader(f):
            key = (row["athlete"], row["event"])
            # Keep the best if duplicates
            existing = prs.get(key)
            if existing is None:
                prs[key] = row["pr"]
            elif _is_field_event(row["event"]):
                if _parse_field_mark_to_inches(row["pr"]) and (
                    _parse_field_mark_to_inches(existing) is None
                    or _parse_field_mark_to_inches(row["pr"]) > _parse_field_mark_to_inches(existing)
                ):
                    prs[key] = row["pr"]
            else:
                if _parse_mark_to_seconds(row["pr"]) and (
                    _parse_mark_to_seconds(existing) is None
                    or _parse_mark_to_seconds(row["pr"]) < _parse_mark_to_seconds(existing)
                ):
                    prs[key] = row["pr"]
    return prs


def _load_sbs(sb_csv: str) -> dict:
    """
    Load season bests from CSV. Returns dict keyed by (athlete_or_relay_key, event) -> sb_mark.
    """
    sbs = {}
    if not os.path.exists(sb_csv):
        return sbs
    with open(sb_csv, newline="") as f:
        for row in csv.DictReader(f):
            key = (row["key"], row["event"])
            sbs[key] = row["sb"]
    return sbs


def _save_sbs(sbs: dict, sb_csv: str):
    """Write season bests dict back to CSV."""
    with open(sb_csv, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["key", "event", "sb"])
        writer.writeheader()
        for (k, event), sb in sorted(sbs.items()):
            writer.writerow({"key": k, "event": event, "sb": sb})


def _save_prs(prs: dict, pr_csv: str):
    """
    Update athlete_prs.csv with new or improved PRs from the prs dict.
    Updates any existing row whose (athlete, event) mark is beaten.
    Inserts new outdoor rows for athlete/event combos not yet in the file.
    """
    fieldnames = ["athlete", "event", "season_type", "pr"]
    rows = []
    seen = set()

    if os.path.exists(pr_csv):
        with open(pr_csv, newline="") as f:
            for row in csv.DictReader(f):
                key = (row["athlete"], row["event"])
                if key in prs:
                    # Update every row for this athlete/event to the best mark
                    row["pr"] = prs[key]
                    seen.add(key)
                rows.append(row)

    # Insert new outdoor entries for athlete/event combos not yet in the file
    for (athlete, event), mark in sorted(prs.items()):
        if (athlete, event) not in seen:
            rows.append({"athlete": athlete, "event": event, "season_type": "outdoor", "pr": mark})

    with open(pr_csv, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def _check_pr_sb(result: dict, event_name: str, is_relay: bool,
                  prs: dict, sbs: dict,
                  orig_prs: dict, orig_sbs: dict,
                  redo: bool = False) -> tuple[bool, bool]:
    """
    Check if a result is a PR and/or SB.
    Compares against orig_prs/orig_sbs (frozen pre-meet snapshot) for detection,
    but updates prs/sbs (live dicts) for saving.

    With redo=True, also treats a mark that exactly matches the current PR/SB
    as a PR/SB (for re-generating emails after a prior buggy run).
    Returns (is_pr, is_sb).
    """
    mark = result.get("mark", "").strip()
    # Strip qualifying suffixes before any comparison or storage
    mark = re.sub(r'[qQ]$', '', mark)
    if not mark or mark in ("--", "DNS", "DNF", "NH", "SCR", "FS"):
        return False, False

    norm_event = _normalize_event_name(event_name)

    if is_relay:
        squad = result.get("squad", "A")
        sb_key = f"relay_{norm_event}_{squad}"
        is_pr = False  # no relay PRs tracked

        # Check SB against original state
        orig_sb = orig_sbs.get((sb_key, norm_event))
        if orig_sb is not None and (_is_better(mark, orig_sb, norm_event)
                                    or (redo and mark == orig_sb)):
            is_sb = True
        elif orig_sb is None and _is_better(mark, orig_sb or "", norm_event):
            is_sb = False  # first time, don't flag
        else:
            is_sb = False
        # Update live dict for saving
        live_sb = sbs.get((sb_key, norm_event))
        if live_sb is None or _is_better(mark, live_sb, norm_event):
            sbs[(sb_key, norm_event)] = mark
    else:
        athlete = result.get("name", "")
        if not athlete:
            return False, False

        # Check PR against original state
        orig_pr = orig_prs.get((athlete, norm_event))
        is_pr = False
        if orig_pr is not None:
            if _is_better(mark, orig_pr, norm_event) or (redo and mark == orig_pr):
                is_pr = True
        # Update live dict for saving
        live_pr = prs.get((athlete, norm_event))
        if live_pr is None or _is_better(mark, live_pr, norm_event):
            prs[(athlete, norm_event)] = mark

        # Check SB against original state
        sb_key = athlete
        orig_sb = orig_sbs.get((sb_key, norm_event))
        if orig_sb is not None and (_is_better(mark, orig_sb, norm_event)
                                    or (redo and mark == orig_sb)):
            is_sb = True
        else:
            is_sb = False
        # Update live dict for saving
        live_sb = sbs.get((sb_key, norm_event))
        if live_sb is None or _is_better(mark, live_sb, norm_event):
            sbs[(sb_key, norm_event)] = mark

    return is_pr, is_sb


# ══════════════════════════════════════════════════════════════════════════════
# MileSplit source (Playwright-based)
# ══════════════════════════════════════════════════════════════════════════════

def _milesplit_extract_base_url(meet_url: str) -> str:
    m = re.search(r"(https?://(?:www\.)?milesplit\.live/meets/\d+)", meet_url)
    if not m:
        raise ValueError(f"Cannot extract milesplit base URL from: {meet_url}")
    return m.group(1)


def _milesplit_scrape_results_table(page, is_relay: bool, debug: bool) -> list[dict]:
    """Scrape UA results from the first visible app-results-list (scored results)."""
    from playwright.sync_api import sync_playwright  # noqa: F811

    ua_results = []

    results_list = page.query_selector("app-results-list:not([hidden])")
    if not results_list:
        results_list = page.query_selector("app-results-list")
    if not results_list:
        if debug:
            print("    [DEBUG] No app-results-list found on page")
        return []

    rows = results_list.query_selector_all("tbody tr[data-index]")
    if debug:
        print(f"    [DEBUG] Found {len(rows)} result rows in scored table")

    for row in rows:
        text = row.inner_text()
        if not _is_ua_team(text):
            continue

        place_el = row.query_selector("span.place")
        place = None
        if place_el:
            m = re.match(r'(\d+)', place_el.inner_text().strip())
            if m:
                place = int(m.group(1))
        if place is None:
            continue

        mark = ""
        # Find the time/mark span — skip badge-icon spans that may precede it
        for span in row.query_selector_all("td.time span, td.mark span"):
            span_class = span.get_attribute("class") or ""
            if "badge-icon" in span_class or "advancement" in span_class:
                continue
            text = span.inner_text().strip()
            if text and re.match(r'[\d:.\-]', text):
                mark = text
                break

        team_name = ""
        if is_relay:
            relay_el = row.query_selector("div.relay")
            if relay_el:
                team_name = relay_el.inner_text().strip()
        else:
            team_el = row.query_selector(".rec-team, .team")
            if team_el:
                team_name = team_el.inner_text().strip()
            else:
                name_td = row.query_selector("td.name")
                if name_td:
                    team_name = name_td.inner_text().strip()

        squad = _squad(team_name)
        result = {"place": place, "mark": mark, "squad": squad}

        if is_relay:
            result["team"] = team_name
            athletes = []
            for span in row.query_selector_all("span.relay.legs > span"):
                name = span.inner_text().strip()
                if name:
                    athletes.append(name)
            result["athletes"] = athletes
        else:
            name = ""
            for sel in ["a.athlete-name", ".athlete-name", "td.name a", "td.name span.ng-star-inserted"]:
                name_el = row.query_selector(sel)
                if name_el:
                    candidate = name_el.inner_text().strip()
                    if candidate and TEAM_NAME not in candidate and not re.match(r'^\d', candidate):
                        name = candidate
                        break
            if not name:
                name_td = row.query_selector("td.name")
                if name_td:
                    for line in name_td.inner_text().strip().split('\n'):
                        line = line.strip()
                        if line and TEAM_NAME not in line and not re.match(r'^\d', line) and len(line) > 2:
                            name = line
                            break
            result["name"] = name

        ua_results.append(result)

    return ua_results


def scrape_meet_milesplit(meet_url: str, debug: bool) -> list[dict]:
    """Scrape milesplit.live using Playwright (browser-based)."""
    from playwright.sync_api import sync_playwright

    base_url = _milesplit_extract_base_url(meet_url)
    schedule_url = f"{base_url}/schedule"

    print(f"Source: milesplit")
    print(f"Schedule URL: {schedule_url}\n")

    output_events = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            viewport={"width": 1280, "height": 900},
            user_agent=(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )
        page = context.new_page()

        print("Loading schedule page...")
        page.goto(schedule_url, wait_until="domcontentloaded", timeout=30000)
        page.wait_for_selector(".event-component", state="attached", timeout=30000)
        time.sleep(2)

        # Expand collapsed sections
        page.evaluate("""() => {
            document.querySelectorAll('.collapse-chevron.collapsed').forEach(chevron => {
                const header = chevron.closest('.section-header');
                if (header) header.click();
            });
        }""")
        time.sleep(1)

        if debug:
            page.screenshot(path="debug_schedule.png")
            print("[DEBUG] Saved debug_schedule.png")

        event_rows = page.query_selector_all(".event-component")
        print(f"Found {len(event_rows)} total event rows")

        girls_events = []
        for i, row in enumerate(event_rows):
            name_el = row.query_selector(".event-name")
            if not name_el:
                continue
            event_name = name_el.inner_text().strip()
            if event_name.lower().startswith("girls"):
                girls_events.append((i, event_name))

        # Deduplicate (schedule may list events twice)
        seen = set()
        girls_events = [(i, n) for i, n in girls_events if n not in seen and not seen.add(n)]
        print(f"Found {len(girls_events)} Girls event(s)\n")

        for event_idx, event_name in girls_events:
            is_relay = any(x in event_name.lower() for x in ["relay", "4x"])
            print(f"  {event_name} ...", end="", flush=True)

            event_rows = page.query_selector_all(".event-component")
            if event_idx >= len(event_rows):
                print(" SKIP (row not found)")
                continue

            event_rows[event_idx].evaluate("el => el.click()")

            try:
                page.wait_for_selector(
                    "app-results-list tbody tr[data-index]",
                    state="attached", timeout=10000,
                )
            except Exception:
                print(" no results table found")
                page.goto(schedule_url, wait_until="domcontentloaded", timeout=30000)
                page.wait_for_selector(".event-component", state="attached", timeout=30000)
                time.sleep(1)
                continue

            time.sleep(1)

            if debug:
                safe_name = re.sub(r'[^a-zA-Z0-9]', '_', event_name)
                page.screenshot(path=f"debug_event_{safe_name}.png")
                with open(f"debug_event_{safe_name}.html", "w") as f:
                    f.write(page.content())
                print(f"\n    [DEBUG] Saved debug_event_{safe_name}.png/.html")

            ua_results = _milesplit_scrape_results_table(page, is_relay, debug)
            ua_results.sort(key=lambda r: r.get("place", 9999))
            print(f" {len(ua_results)} UA result(s)")

            if debug and ua_results:
                for r in ua_results:
                    print(f"    {r}")

            if ua_results:
                output_events.append({
                    "event_name": event_name,
                    "is_relay": is_relay,
                    "results": ua_results,
                })

            page.goto(schedule_url, wait_until="domcontentloaded", timeout=30000)
            page.wait_for_selector(".event-component", state="attached", timeout=30000)
            time.sleep(1)

        browser.close()

    return output_events


# ══════════════════════════════════════════════════════════════════════════════
# FinishTiming source (Firebase REST API — no browser needed)
# ══════════════════════════════════════════════════════════════════════════════

FINISHTIMING_RTDB = "https://track-scoreboard-default-rtdb.firebaseio.com"


def _ft_fetch_json(path: str) -> any:
    """Fetch JSON from the Firebase Realtime Database."""
    url = f"{FINISHTIMING_RTDB}{path}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read())


def _ft_normalize_event_name(event_data: dict) -> str:
    """
    Normalize finishtiming event names.
    They use 'Womens' instead of 'Girls' — convert for consistency.
    """
    # Get name from the first round
    for round_name, round_data in event_data.get("rounds", {}).items():
        name = round_data.get("name", "")
        if name:
            return name
    return ""


def _ft_is_girls_event(event_data: dict) -> bool:
    """Return True if the event is a women's/girls event."""
    return event_data.get("gender") in ("W", "G")


def scrape_meet_finishtiming(meet_url: str, debug: bool) -> list[dict]:
    """Scrape finishtiming.trackscoreboard.com using Firebase REST API."""
    meet_id = _extract_meet_id(meet_url)

    # Detect the company prefix from the URL
    m = re.match(r'https?://(\w+)\.trackscoreboard\.com', meet_url)
    company = m.group(1) if m else "finishtiming"

    print(f"Source: finishtiming (company={company})")
    print(f"Meet ID: {meet_id}\n")

    base_path = f"/trackscoreboard/{company}/meets/{meet_id}/meet"

    print("Fetching events...")
    events = _ft_fetch_json(f"{base_path}/events.json")

    print("Fetching all results...")
    all_results = _ft_fetch_json(f"{base_path}/results.json")

    if not events:
        print("ERROR: No events found.")
        return []

    # events is a list indexed by event ID (index 0 is null)
    girls_events = []
    for i, event in enumerate(events):
        if event is None:
            continue
        if not _ft_is_girls_event(event):
            continue
        event_name = _ft_normalize_event_name(event)
        is_relay = event.get("relay", 0) == 1
        # Use array index (not event "id") — results list is indexed the same way
        girls_events.append((i, event_name, is_relay))

    print(f"Found {len(girls_events)} Girls/Womens event(s)\n")

    output_events = []

    for event_id, event_name, is_relay in girls_events:
        print(f"  {event_name} ...", end="", flush=True)

        # Get results for this event
        event_results = None
        if isinstance(all_results, list) and event_id < len(all_results):
            event_results = all_results[event_id]
        elif isinstance(all_results, dict):
            event_results = all_results.get(str(event_id))

        if not event_results:
            print(" no results")
            continue

        ua_results = _ft_extract_ua_results(event_results, is_relay, debug)
        ua_results.sort(key=lambda r: r.get("place", 9999))
        print(f" {len(ua_results)} UA result(s)")

        if debug and ua_results:
            for r in ua_results:
                print(f"    {r}")

        if ua_results:
            output_events.append({
                "event_name": event_name,
                "is_relay": is_relay,
                "results": ua_results,
            })

    return output_events


def _ft_extract_ua_results(event_results: dict, is_relay: bool, debug: bool) -> list[dict]:
    """Extract Ursuline Academy results from a finishtiming event results dict."""
    ua_results = []

    # Structure: {roundName: {results: [{results: [...]}]}} or {roundName: [{results: [...]}]}
    for round_name, round_data in event_results.items():
        sections = []
        if isinstance(round_data, dict):
            sections = round_data.get("results", [])
        elif isinstance(round_data, list):
            sections = round_data

        for section in sections:
            if not isinstance(section, dict):
                continue
            results = section.get("results", [])
            for r in results:
                if not isinstance(r, dict):
                    continue
                team = r.get("teamName", "")
                if not _is_ua_team(team):
                    continue

                place = r.get("place")
                if place is None or place == 0:
                    continue

                mark = r.get("mark", "")
                status = r.get("status", "")

                # Handle DNS/DNF/etc
                if status in ("DNS", "DNF", "NH", "SCR", "FS"):
                    mark = status

                relay_letter = r.get("relay_team_letter", "A")
                squad = "B" if relay_letter == "B" else "A"

                result = {"place": place, "mark": mark, "squad": squad}

                if is_relay:
                    team_display = f"{team} \"{relay_letter}\""
                    result["team"] = team_display
                    athletes = []
                    for a in r.get("athletes", []):
                        fname = a.get("fname", "")
                        lname = a.get("lname", "")
                        full = f"{fname} {lname}".strip()
                        if full:
                            athletes.append(full)
                    result["athletes"] = athletes
                else:
                    fname = r.get("fname", "")
                    lname = r.get("lname", "")
                    result["name"] = f"{fname} {lname}".strip()

                ua_results.append(result)

    return ua_results


# ══════════════════════════════════════════════════════════════════════════════
# Summary Builder & HTML Output (shared)
# ══════════════════════════════════════════════════════════════════════════════

def mark_label(event_name: str) -> str:
    n = event_name.lower()
    if "jump" in n: return "jump"
    if "vault" in n: return "vault"
    if "throw" in n or "put" in n or "discus" in n or "javelin" in n: return "mark"
    return "time"


def _pr_sb_label_prefix(is_pr: bool, is_sb: bool) -> str:
    """Return a prefix like 'PR ' or 'SB ' to insert before the label word.
    PR implies SB so only show PR when both are true."""
    if is_pr:
        return '<strong>PR</strong> '
    if is_sb:
        return '<strong>SB</strong> '
    return ""


def build_individual_sentence(r: dict, label: str) -> str:
    name = r.get("name", "Unknown")
    p = r["place"]
    mark = r["mark"]
    dns = mark in ("--", "DNS", "DNF", "NH", "SCR", "FS", "")

    b = "<strong>{}</strong>".format

    if dns:
        status = (
            "did not start (DNS)" if mark == "DNS" else
            "did not finish (DNF)" if mark == "DNF" else
            "did not clear (NH)" if mark == "NH" else
            "scratched (SCR)" if mark == "SCR" else
            "false start (FS)" if mark == "FS" else
            "did not compete"
        )
        return f"{b(name)} {status}"

    prefix = _pr_sb_label_prefix(r.get("is_pr", False), r.get("is_sb", False))

    intro = (
        f"{b(name)} won the event outright" if p == 1
        else f"{b(name)} finished {b(place_str(p))} overall"
    )
    return f"{intro} with a {prefix}{label} of {b(mark)}"


def build_relay_sentence(r: dict, label: str, has_multiple_squads: bool) -> str:
    p = r["place"]
    mark = r["mark"]
    athletes = r.get("athletes", [])
    squad = r.get("squad", "A")

    b = "<strong>{}</strong>".format

    if has_multiple_squads:
        team_label = f"The {'B' if squad == 'B' else 'A'} team"
    else:
        team_label = "The team"
    athlete_str = (" of " + ", ".join(b(a) for a in athletes)) if athletes else ""

    prefix = _pr_sb_label_prefix(False, r.get("is_sb", False))

    intro = (
        f"{team_label}{athlete_str} won the event" if p == 1
        else f"{team_label}{athlete_str} finished {b(place_str(p))} overall"
    )
    return f"{intro} with a {prefix}{label} of {b(mark)}"


def generate_event_html(event: dict) -> str:
    name = event["event_name"]
    results = sorted(event["results"], key=lambda r: r.get("place", 9999))
    label = mark_label(name)
    is_relay = event["is_relay"]

    if not results:
        return ""

    b = "<strong>{}</strong>".format
    sentences = []

    has_multiple_squads = is_relay and len(set(r.get("squad", "A") for r in results)) > 1

    for r in results:
        if is_relay:
            sentences.append(build_relay_sentence(r, label, has_multiple_squads))
        else:
            sentences.append(build_individual_sentence(r, label))

    body = ". ".join(sentences) + "."
    return f"<p>{b(name + ':')}&nbsp;{body}</p>\n"


def write_html(meet_name: str, events: list, out_path: str):
    event_blocks = [generate_event_html(e) for e in events if e["results"]]

    html = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body {{
      font-family: Arial, sans-serif;
      font-size: 14px;
      line-height: 1.7;
      max-width: 800px;
      margin: 40px auto;
      padding: 0 20px;
    }}
    p {{ margin: 6px 0; }}
  </style>
</head>
<body>
<p><strong>URSULINE ACADEMY — {meet_name}</strong></p>
<hr style="margin:10px 0; border:none; border-top:1px solid #ccc;">
{"".join(event_blocks)}
</body>
</html>"""

    with open(out_path, "w") as f:
        f.write(html)

    print(f"\nHTML written to: {os.path.abspath(out_path)}")
    print("Open in browser → Cmd+A → Cmd+C → paste into email.")


# ══════════════════════════════════════════════════════════════════════════════
# Post results to backend API
# ══════════════════════════════════════════════════════════════════════════════

def _normalize_name(name: str) -> str:
    """Normalize a name for fuzzy matching: lowercase, alpha-only, sorted tokens."""
    if "," in name:
        parts = name.split(",", 1)
        name = parts[1].strip() + " " + parts[0].strip()
    name = re.sub(r"[^a-zA-Z ]", "", name).lower()
    return " ".join(sorted(name.split()))


def _find_runner_id(scraped_name: str, roster: dict) -> int | None:
    """Match a scraped name to a runner ID using exact then fuzzy matching."""
    key = _normalize_name(scraped_name)
    if key in roster:
        return roster[key]
    matches = get_close_matches(key, roster.keys(), n=1, cutoff=0.82)
    if matches:
        print(f"    [FUZZY MATCH] '{scraped_name}' → '{matches[0]}'")
        return roster[matches[0]]
    return None


def _fetch_roster(base_url: str) -> dict:
    """Returns {normalized_name: runner_id} for all runners."""
    import requests
    resp = requests.get(f"{base_url}/xc/allRunners/", timeout=10)
    resp.raise_for_status()
    return {_normalize_name(r["name"]): r["id"] for r in resp.json()}


def _fetch_track_meet(base_url: str, meet_name: str) -> dict | None:
    """Find a track meet by name. Returns {name, date} using the most recent date."""
    import requests
    resp = requests.get(f"{base_url}/track/getMeetNames", params={"team": "UA"}, timeout=10)
    resp.raise_for_status()
    data = resp.json()
    meets = data.get("meets", data) if isinstance(data, dict) else data

    def _best_match(meet):
        dates = sorted(meet.get("dates", []), reverse=True)
        date = dates[0] if dates else None
        return {"name": meet.get("meetName", meet.get("name", "")).strip(), "date": date}

    # Exact match (case-insensitive, strip whitespace)
    for meet in meets:
        m_name = meet.get("meetName", meet.get("name", "")).strip()
        if m_name.lower() == meet_name.strip().lower():
            return _best_match(meet)
    # Partial match
    for meet in meets:
        m_name = meet.get("meetName", meet.get("name", "")).strip()
        if meet_name.strip().lower() in m_name.lower() or m_name.lower() in meet_name.strip().lower():
            return _best_match(meet)
    return None


def _event_distance_meters(norm_event: str) -> int:
    """Convert normalized event name to distance in meters."""
    dist_map = {
        "400m": 400, "600m": 600, "800m": 800,
        "1500m": 1500, "1600m": 1600, "mile": 1609,
        "3000m": 3000, "3000m Steeplechase": 3000,
        "3200m": 3200, "2 mile": 3218,
    }
    return dist_map.get(norm_event, 0)


def _is_postable_event(event_name: str) -> bool:
    """Return True if the event is a distance event eligible for auto-posting."""
    norm = _normalize_event_name(event_name)
    return norm.lower() in {e.lower() for e in POSTABLE_EVENTS}


def _post_results_to_api(all_events: list[dict], meet_name: str, debug: bool):
    """Post individual distance-event results to the backend API."""
    import requests

    print("\n--- Posting results to backend API ---")

    # Fetch roster for name → runner ID matching
    try:
        roster = _fetch_roster(BASE_API_URL)
    except Exception as e:
        print(f"ERROR: Failed to fetch roster: {e}")
        return
    print(f"Loaded {len(roster)} runners from backend")

    # Find the track meet by name
    meet = _fetch_track_meet(BASE_API_URL, meet_name)
    if not meet:
        print(f"ERROR: No track meet found matching '{meet_name}' in backend")
        return
    print(f"Matched meet: '{meet['name']}' (date: {meet['date']})")

    # Derive season year from meet date
    date_str = str(meet["date"])  # e.g. "2026-04-10" or epoch millis
    if isinstance(meet["date"], (int, float)):
        # epoch millis
        from datetime import datetime
        dt = datetime.fromtimestamp(meet["date"] / 1000)
        season = str(dt.year)
    else:
        season = date_str[:4]
    print(f"Season: {season}")

    # Collect individual distance-event results grouped by runner
    runner_results = {}  # runner_name -> list of (norm_event, result)
    for event in all_events:
        if event["is_relay"]:
            continue
        if not _is_postable_event(event["event_name"]):
            if debug:
                print(f"  Skipping non-distance event: {event['event_name']}")
            continue
        norm_event = _normalize_event_name(event["event_name"])
        for r in event["results"]:
            name = r.get("name", "")
            mark = r.get("mark", "")
            if not name or not mark or mark in ("--", "DNS", "DNF", "NH", "SCR", "FS"):
                continue
            runner_results.setdefault(name, []).append((norm_event, r))

    if not runner_results:
        print("No distance-event results to post.")
        return

    posted = 0
    skipped = 0
    for runner_name, results in runner_results.items():
        runner_id = _find_runner_id(runner_name, roster)
        if runner_id is None:
            print(f"  SKIP: No runner match for '{runner_name}'")
            skipped += 1
            continue

        track_results = []
        for norm_event, r in results:
            track_results.append({
                "uuid": str(uuid_mod.uuid4()),
                "event": norm_event,
                "distance": _event_distance_meters(norm_event),
                "time": r["mark"],
                "place": r.get("place", 0),
                "splits": [],
                "isSplit": False,
            })

        payload = {
            "season": season,
            "runnerId": runner_id,
            "meetName": meet["name"],
            "meetId": "",
            "results": track_results,
        }

        try:
            resp = requests.post(
                f"{BASE_API_URL}/track/meetResults/createForRunner",
                json=payload,
                timeout=15,
            )
            resp.raise_for_status()
            events_str = ", ".join(f"{norm} ({r['mark']})" for norm, r in results)
            print(f"  POSTED: {runner_name} (id={runner_id}) → {events_str}")
            posted += 1
        except Exception as e:
            print(f"  ERROR posting {runner_name}: {e}")

    print(f"\nPost summary: {posted} runner(s) posted, {skipped} skipped")


# ══════════════════════════════════════════════════════════════════════════════
# TimingSpot / AthleticLIVE source (Playwright + blob REST API)
# ══════════════════════════════════════════════════════════════════════════════

TIMINGSPOT_BLOB = "https://athleticlive.blob.core.windows.net/$web"


def _ts_fetch_blob(path: str) -> dict | None:
    """Fetch JSON from the AthleticLIVE blob storage."""
    url = f"{TIMINGSPOT_BLOB}/{path}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read())
            return data.get("_source", data)
    except Exception:
        return None


def _ts_extract_meet_id(meet_url: str) -> str:
    m = re.search(r"/meets/(\d+)", meet_url)
    if not m:
        raise ValueError(f"Cannot extract timingspot meet ID from: {meet_url}")
    return m.group(1)


def _ts_extract_ua_individual(event_data: dict) -> list[dict]:
    """Extract Ursuline Academy results from a timingspot individual event."""
    results = event_data.get("r", [])
    ua_results = []
    for r in results:
        athlete = r.get("a", {})
        team = athlete.get("t", {})
        team_name = team.get("n", "") or team.get("f", "")
        if TEAM_NAME.lower() not in team_name.lower():
            continue
        mark = re.sub(r'[qQ]$', '', r.get("m", "").strip())
        place_str = r.get("p", "")
        try:
            place = int(place_str)
        except (ValueError, TypeError):
            place = 0
        result = {
            "place": place,
            "mark": mark,
            "squad": "A",
            "name": athlete.get("n", ""),
        }
        ua_results.append(result)
    return ua_results


def _ts_extract_ua_relay(event_data: dict) -> list[dict]:
    """Extract Ursuline Academy results from a timingspot relay event."""
    results = event_data.get("rts", [])
    ua_results = []
    for r in results:
        team_info = r.get("t", {})
        team_name = team_info.get("n", "") or team_info.get("f", "")
        if TEAM_NAME.lower() not in team_name.lower():
            continue
        mark = re.sub(r'[qQ]$', '', r.get("m", "").strip())
        place_str = r.get("p", "")
        try:
            place = int(place_str)
        except (ValueError, TypeError):
            place = 0

        # Extract athlete names from relay members
        athletes = []
        for member in r.get("rm", []):
            a = member.get("a", {})
            name = a.get("n", "")
            if name:
                athletes.append(name)

        # Determine squad letter from relay designation
        squad = r.get("rd", "A") or "A"

        result = {
            "place": place,
            "mark": mark,
            "squad": squad,
            "team": team_name,
            "athletes": athletes,
        }
        ua_results.append(result)
    return ua_results


def scrape_meet_timingspot(meet_url: str, debug: bool) -> list[dict]:
    """Scrape timingspot/AthleticLIVE using Playwright for event list + blob API for results."""
    from playwright.sync_api import sync_playwright

    meet_id = _ts_extract_meet_id(meet_url)
    print(f"Source: timingspot (AthleticLIVE)")
    print(f"Meet ID: {meet_id}\n")

    output_events = []

    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        page = browser.new_page()

        # Use the original domain (timingspot.com or athletic.net)
        base_m = re.match(r'(https?://[^/]+)', meet_url)
        base_origin = base_m.group(1) if base_m else "https://live.timingspot.com"
        events_url = f"{base_origin}/meets/{meet_id}/events"
        page.goto(events_url, wait_until="networkidle", timeout=30000)
        time.sleep(4)

        if debug:
            page.screenshot(path="debug_ts_schedule.png", full_page=True)
            print("[DEBUG] Saved debug_ts_schedule.png")

        # Extract event rows: each has a link like /meets/{id}/events/(individual|relay)/{eventId}
        rows = page.query_selector_all(".events-table--row")
        girls_events = []
        for row in rows:
            link = row.query_selector("a[href]")
            if not link:
                continue
            href = link.get_attribute("href") or ""
            text = " ".join(row.inner_text().split())

            m = re.search(r"/(individual|relay)/(\d+)", href)
            if not m:
                continue
            if "Girls" not in text:
                continue

            etype = m.group(1)
            eid = m.group(2)
            girls_events.append((etype, eid, text))

        browser.close()

    print(f"Found {len(girls_events)} Girls event(s)\n")

    for etype, eid, row_text in girls_events:
        # Derive event name from row text (strip "Results"/"Live" prefix and time suffix)
        name_match = re.search(r"(?:Results|Live|Upcoming)\s+(Girls\s+.+?)(?:\s+(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)\b|\s*$)", row_text)
        event_name = name_match.group(1).strip() if name_match else row_text.strip()

        print(f"  {event_name} ...", end="", flush=True)

        blob_type = "ind_res_list" if etype == "individual" else "rel_res_list"
        event_data = _ts_fetch_blob(f"{blob_type}/_doc/{eid}")

        if not event_data:
            print(" no data")
            continue

        is_relay = etype == "relay"
        if is_relay:
            ua_results = _ts_extract_ua_relay(event_data)
        else:
            ua_results = _ts_extract_ua_individual(event_data)

        ua_results.sort(key=lambda r: r.get("place", 9999))
        print(f" {len(ua_results)} UA result(s)")

        if debug and ua_results:
            for r in ua_results:
                print(f"    {r}")

        if ua_results:
            output_events.append({
                "event_name": event_name,
                "is_relay": is_relay,
                "results": ua_results,
            })

    return output_events


# ══════════════════════════════════════════════════════════════════════════════
# Main
# ══════════════════════════════════════════════════════════════════════════════

SOURCES = {
    "milesplit": scrape_meet_milesplit,
    "finishtiming": scrape_meet_finishtiming,
    "timingspot": scrape_meet_timingspot,
}


def _detect_source(meet_url: str) -> str | None:
    """Try to auto-detect the source from the URL."""
    if "milesplit.live" in meet_url:
        return "milesplit"
    if "trackscoreboard.com" in meet_url:
        return "finishtiming"
    if "timingspot.com" in meet_url or "athletic.net" in meet_url:
        return "timingspot"
    return None


def _annotate_pr_sb(all_events: list[dict], prs: dict, sbs: dict,
                    debug: bool, redo: bool = False):
    """Annotate each result with is_pr and is_sb flags. Updates prs/sbs in place."""
    # Snapshot the pre-meet state so re-runs detect the same PRs/SBs
    orig_prs = dict(prs)
    orig_sbs = dict(sbs)
    pr_count = 0
    sb_count = 0
    for event in all_events:
        for r in event["results"]:
            is_pr, is_sb = _check_pr_sb(
                r, event["event_name"], event["is_relay"], prs, sbs,
                orig_prs, orig_sbs, redo=redo
            )
            r["is_pr"] = is_pr
            r["is_sb"] = is_sb
            if is_pr:
                pr_count += 1
            if is_sb:
                sb_count += 1
    if pr_count or sb_count:
        print(f"\nPR/SB summary: {pr_count} PR(s), {sb_count} SB(s) detected")


def main(source: str, meet_url: str, meet_name: str, debug: bool,
         redo: bool = False, post: bool = False):
    print(f"Fetching '{meet_name}' results ...")
    print(f"URL: {meet_url}\n")

    scrape_fn = SOURCES.get(source)
    if not scrape_fn:
        print(f"ERROR: Unknown source '{source}'. Must be one of: {', '.join(SOURCES)}")
        sys.exit(1)

    all_events = scrape_fn(meet_url, debug)

    if all_events:
        # Load PRs and season bests
        prs = _load_prs(PR_CSV)
        sbs = _load_sbs(SB_CSV)
        if prs:
            print(f"Loaded {len(prs)} PRs from {PR_CSV}")
        else:
            print(f"No PR data found at {PR_CSV} — run scrape_athlete_prs.py first to enable PR detection")
        if sbs:
            print(f"Loaded {len(sbs)} season bests from {SB_CSV}")

        # Annotate results with PR/SB flags
        _annotate_pr_sb(all_events, prs, sbs, debug, redo=redo)

        # Save updated PRs and season bests
        _save_prs(prs, PR_CSV)
        print(f"PRs saved: {len(prs)} total entries in {PR_CSV}")
        _save_sbs(sbs, SB_CSV)
        print(f"Season bests saved: {len(sbs)} total entries in {SB_CSV}")

        out_path = f"track_summary_{meet_name.replace(' ', '_')}.html"
        write_html(meet_name, all_events, out_path)

        # Post results to backend API if requested
        if post:
            _post_results_to_api(all_events, meet_name, debug)
    else:
        print("\nNo Ursuline Academy results found.")
        print("Tips:")
        if source == "milesplit":
            print("  - Run with --debug to see screenshots and HTML dumps")
        else:
            print("  - Run with --debug to see raw result data")
        print("  - Verify the meet URL is correct")


if __name__ == "__main__":
    args = sys.argv[1:]
    debug = "--debug" in args
    redo = "--redo" in args
    post = "--post" in args
    args = [a for a in args if a not in ("--debug", "--redo", "--post")]

    # Parse --source flag
    source = None
    filtered_args = []
    i = 0
    while i < len(args):
        if args[i] == "--source" and i + 1 < len(args):
            source = args[i + 1].lower()
            i += 2
        else:
            filtered_args.append(args[i])
            i += 1
    args = filtered_args

    if len(args) < 2:
        print("Usage: python scrape_track_summary.py --source <milesplit|finishtiming> <meet_url> <meet_name> [--debug]")
        print()
        print("The --source flag is optional if the URL can be auto-detected.")
        print()
        print("Examples:")
        print('  python scripts/scrape_track_summary.py --source milesplit \\')
        print('      "https://milesplit.live/meets/718150/schedule" "Badin Invitational"')
        print()
        print('  python scripts/scrape_track_summary.py --source finishtiming \\')
        print('      "https://finishtiming.trackscoreboard.com/meets/645654/events" "Madeira Invitational"')
        sys.exit(1)

    meet_url = args[0]
    meet_name = args[1]

    # Auto-detect source if not specified
    if source is None:
        source = _detect_source(meet_url)
        if source is None:
            print("ERROR: Cannot auto-detect source from URL. Please specify --source <milesplit|finishtiming>")
            sys.exit(1)
        print(f"Auto-detected source: {source}")

    if redo:
        print("--redo mode: marks matching current PR/SB will be flagged as PR/SB")
    if post:
        print("--post mode: distance-event results will be posted to backend API")
    main(source=source, meet_url=meet_url, meet_name=meet_name, debug=debug, redo=redo, post=post)
