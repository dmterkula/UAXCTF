#!/usr/bin/env python3
"""
XC Meet Results Scraper
Scrapes Ursuline Academy results from finishtiming.trackscoreboard.com
and loads them into the UAXCTF backend.

Setup (one time):
    cd /path/to/UAXCTF
    python3 -m venv scripts/venv
    source scripts/venv/bin/activate
    pip install playwright requests
    playwright install chromium

Each session:
    source scripts/venv/bin/activate

Usage:
    python scripts/scrape_meet_results.py <meet_url> <meet_name> <season> [--dry-run] [--debug]

Examples:
    # Preview without writing to backend (always do this first):
    python scripts/scrape_meet_results.py \
        "https://finishtiming.trackscoreboard.com/meets/583161/events" \
        "Troy" 2024 --dry-run

    # Load results live:
    python scripts/scrape_meet_results.py \
        "https://finishtiming.trackscoreboard.com/meets/583161/events" \
        "Troy" 2024

    # Dump rendered HTML to inspect page structure:
    python scripts/scrape_meet_results.py \
        "https://finishtiming.trackscoreboard.com/meets/583161/events" \
        "Troy" 2024 --debug
"""

import sys
import re
import requests
from difflib import get_close_matches
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeout

# ── Config ────────────────────────────────────────────────────────────────────

BASE_API_URL = "http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com"
TEAM_NAME = "Ursuline Academy"

# ms to wait after navigation for Angular to finish rendering
PAGE_LOAD_WAIT_MS = 1500

# ── Name Normalization ────────────────────────────────────────────────────────

def normalize_name(name: str) -> str:
    """
    Handles 'LAST, FIRST', 'First Last', extra whitespace, punctuation.
    Lowercases, strips non-alpha, sorts tokens so order doesn't matter.
    """
    if "," in name:
        parts = name.split(",", 1)
        name = parts[1].strip() + " " + parts[0].strip()
    name = re.sub(r"[^a-zA-Z ]", "", name).lower()
    return " ".join(sorted(name.split()))


def find_runner_id(scraped_name: str, roster: dict) -> int | None:
    key = normalize_name(scraped_name)
    if key in roster:
        return roster[key]
    matches = get_close_matches(key, roster.keys(), n=1, cutoff=0.82)
    if matches:
        print(f"  [FUZZY MATCH] '{scraped_name}' → '{matches[0]}'")
        return roster[matches[0]]
    return None


# ── API Helpers ───────────────────────────────────────────────────────────────

def fetch_roster(base_url: str) -> dict:
    """Returns {normalized_name: runner_id} for all runners."""
    resp = requests.get(f"{base_url}/xc/allRunners/", timeout=10)
    resp.raise_for_status()
    return {normalize_name(r["name"]): r["id"] for r in resp.json()}


def post_meet_result(base_url: str, payload: dict) -> None:
    resp = requests.post(
        f"{base_url}/xc/meetResults/createForRunner",
        json=payload,
        timeout=10
    )
    resp.raise_for_status()


def fetch_meet_id(base_url: str, meet_name: str, season: str) -> int | None:
    resp = requests.get(f"{base_url}/xc/meets/getBySeason", params={"season": season}, timeout=10)
    resp.raise_for_status()
    for m in resp.json():
        if m.get("name", "").strip().lower() == meet_name.strip().lower():
            return m["id"]
    return None


def fetch_divisions(base_url: str, meet_id: int) -> list:
    resp = requests.get(f"{base_url}/xc/meetDivisions", params={"meetId": meet_id}, timeout=10)
    resp.raise_for_status()
    return sorted(resp.json(), key=lambda d: d["sequence"])


def build_division_rank_map(scraped: list, divisions: list) -> dict:
    """
    Maps each scraped race's divisionSequence -> configured division id, by RELATIVE rank
    (1st race with our results <-> 1st configured division, 2nd <-> 2nd, ...) rather than by
    absolute start-time proximity. Absolute-time matching breaks whenever the meet actually ran
    earlier/later than the configured times by more than about half the gap between divisions --
    relative order is what's actually reliable here, since both the timing site's race order and
    the order a coach adds divisions in reflect the real running order of the meet.

    Only returns a mapping when the counts line up exactly -- otherwise it's not safe to guess,
    and results fall back to the backend's own (start-time/sequence/name) matching.
    """
    distinct_race_sequences = sorted({r["divisionSequence"] for r in scraped})
    if not divisions or len(distinct_race_sequences) != len(divisions):
        return {}
    return {race_seq: division["id"] for race_seq, division in zip(distinct_race_sequences, divisions)}


# ── Playwright Scraper ────────────────────────────────────────────────────────

def collect_race_urls(page, meet_url: str, debug: bool) -> list:
    """
    Phase 1: Navigate to the events listing page and collect all individual
    race URLs. Returns list of (race_url, division_label) tuples.

    Individual race pages follow the pattern:
        /meets/{meetId}/events/{raceId}
    """
    base_origin = re.match(r"(https?://[^/]+)", meet_url).group(1)
    meet_id = re.search(r"/meets/(\d+)", meet_url).group(1)

    print(f"Loading events page: {meet_url}")
    page.goto(meet_url, wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(PAGE_LOAD_WAIT_MS)

    if debug:
        with open("debug_events_page.html", "w") as f:
            f.write(page.content())
        print("[DEBUG] Wrote debug_events_page.html")

    # Find all anchor tags linking to individual race result pages
    race_pattern = f"/meets/{meet_id}/events/"
    links = page.query_selector_all(f'a[href*="{race_pattern}"]')

    races = []
    seen = set()

    for link in links:
        href = link.get_attribute("href") or ""

        # Skip the events listing page itself (live.athletic.net race links are /events/xc/{raceId})
        if not re.search(r"/events/(xc/)?\d+", href):
            continue

        race_url = href if href.startswith("http") else base_origin + href
        if race_url in seen:
            continue
        seen.add(race_url)

        # Try to grab a division label + the race's actual start time from the surrounding context.
        # The start time is the important signal for matching to pre-configured divisions -- race
        # *position* on this page counts every race at the meet (including other genders/schools),
        # so a team that only runs Varsity + Open won't naturally line up with raw position.
        division = _get_division_label(link, meet_url)
        start_time = _get_race_start_time(link)
        races.append((race_url, division, start_time))
        print(f"  Found: {division}" + (f" ({start_time})" if start_time else "") + f"  →  {race_url}")

    return races


def _get_race_start_time(link_el) -> str | None:
    """
    Extract this race's start time (24h "HH:MM") from the "Race N - H:MM am/pm" label
    that sits alongside the race link on the events listing page.
    """
    try:
        text = link_el.evaluate("""el => {
            const numEl = el.querySelector('.event-number');
            return numEl ? numEl.innerText.trim() : '';
        }""")
        m = re.search(r"(\d{1,2}):(\d{2})\s*([ap]m)", text, re.IGNORECASE)
        if not m:
            return None
        hour, minute, meridiem = int(m.group(1)), int(m.group(2)), m.group(3).lower()
        if meridiem == "pm" and hour != 12:
            hour += 12
        if meridiem == "am" and hour == 12:
            hour = 0
        return f"{hour:02d}:{minute:02d}"
    except Exception:
        return None


def _get_division_label(link_el, fallback: str) -> str:
    """
    Extract the event/division name for a race link on the events listing page.
    trackscoreboard.com renders this as a dedicated `.event-name` element inside
    the link (e.g. "HS Girls Section 1 Varsity - 5K") -- walking up the DOM for a
    "nearby heading" instead grabs the whole race-list nav block, since every race
    link sits inside the same shared container.
    """
    try:
        label = link_el.evaluate("""el => {
            const nameEl = el.querySelector('.event-name');
            if (nameEl) return nameEl.innerText.trim();
            return '';
        }""")
        return label.strip() if label and label.strip() else link_el.inner_text().strip()
    except Exception:
        return link_el.inner_text().strip() or fallback


def scrape_race_page(page, race_url: str, division_name: str, sequence: int, start_time: str | None, debug: bool) -> list:
    """
    Phase 2: Navigate to a single race results page and return all
    Ursuline Academy results as {name, time, place, division, divisionSequence, raceStartTime} dicts.
    """
    try:
        page.goto(race_url, wait_until="networkidle", timeout=30000)
    except PlaywrightTimeout:
        # Live-scoring race pages keep an open socket and never go network-idle;
        # fall back to a plain load and a fixed settle delay instead.
        page.goto(race_url, wait_until="domcontentloaded", timeout=30000)
    page.wait_for_timeout(PAGE_LOAD_WAIT_MS)

    if debug:
        race_id = race_url.rstrip("/").split("/")[-1]
        with open(f"debug_race_{race_id}.html", "w") as f:
            f.write(page.content())
        print(f"  [DEBUG] Wrote debug_race_{race_id}.html")

    is_athletic_live = "live.athletic.net" in race_url

    # Try to get a more precise division name from the page <title>, which trackscoreboard.com
    # renders as "{Race Name} · {Meet Name} · Finish Timing" -- more reliable than DOM headings,
    # since the on-page heading elements sit inside a shared nav block common to every race page.
    # live.athletic.net uses "{Race Name} - {Meet Name} - {Date} | AthleticLIVE" instead.
    try:
        title_text = page.title().strip()
        race_name = title_text.split(" - " if is_athletic_live else " · ")[0].strip()
        if race_name:
            division_name = race_name
    except Exception:
        pass

    # The individual race page often repeats its own start time (e.g. "Start Time: 10:15 am") --
    # prefer this over the listing-page value when present, since it's the most direct source.
    try:
        page_text = page.inner_text("body")
        m = re.search(r"Start Time:\s*(\d{1,2}):(\d{2})\s*([ap]m)", page_text, re.IGNORECASE)
        if not m and is_athletic_live:
            # Race header reads "{Meet Name} | Oct 24, 2026 | 10:00 AM"
            m = re.search(r"\d{4}\s*\|\s*(\d{1,2}):(\d{2})\s*([ap]m)", page_text, re.IGNORECASE)
        if m:
            hour, minute, meridiem = int(m.group(1)), int(m.group(2)), m.group(3).lower()
            if meridiem == "pm" and hour != 12:
                hour += 12
            if meridiem == "am" and hour == 12:
                hour = 0
            start_time = f"{hour:02d}:{minute:02d}"
    except Exception:
        pass

    if is_athletic_live:
        return _scrape_athletic_live_results(page, division_name, sequence, start_time)

    results = []

    # Angular Material table rows (most common for this app)
    rows = page.query_selector_all("mat-row")
    if not rows:
        # Fallback to plain HTML table rows
        rows = page.query_selector_all("tr")

    for row in rows:
        if TEAM_NAME not in row.inner_text():
            continue

        cells = row.query_selector_all("mat-cell") or row.query_selector_all("td")
        if len(cells) < 3:
            continue

        result = _parse_row(cells, division_name, sequence, start_time)
        if result:
            results.append(result)

    return results


def _scrape_athletic_live_results(page, division_name: str, sequence: int, start_time: str | None) -> list:
    """
    live.athletic.net renders results as grid rows (not a table) and pages them 100 at a time
    behind a "1 - 100 | ..." dropdown, so walk every page of the dropdown.
    """
    def parse_rows() -> list:
        return page.evaluate("""(team) => [...document.querySelectorAll('.results-table--row')].map(row => {
            const q = sel => { const el = row.querySelector(sel); return el ? el.innerText.trim() : ''; };
            return {
                place: q('.results-table--place'),
                name: q('.results-table--grid--primary .results-table--grid--title'),
                team: q('.results-table--grid--team-name'),
                time: q('.results-table--grid--secondary .results-table--grid--title'),
            };
        }).filter(r => r.team === team)""", TEAM_NAME)

    raw = parse_rows()
    # The dropdown is rendered more than once (some copies hidden), and other dropdowns (e.g. the
    # empty "Qualifiers" menu) sit before it, so find the page-range menu by its "1 - 100 | ..." items
    # and drive the first copy via JS.
    find_menu_js = """() => [...document.querySelectorAll('app-results .dropdown-menu')]
        .find(m => m.querySelector('.dropdown-item') && /^\\s*\\d+\\s*-\\s*\\d+/.test(m.querySelector('.dropdown-item').innerText))"""
    page_count = page.evaluate(f"""() => {{
        const menu = ({find_menu_js})();
        return menu ? menu.querySelectorAll('.dropdown-item').length : 0;
    }}""")
    for i in range(1, page_count):
        page.evaluate(f"""(i) => ({find_menu_js})()
            .querySelectorAll('.dropdown-item')[i].click()""", i)
        page.wait_for_timeout(PAGE_LOAD_WAIT_MS)
        raw.extend(parse_rows())

    results, seen = [], set()
    for r in raw:
        place_str = re.sub(r"[^0-9]", "", r["place"])
        if not place_str or not r["name"] or not re.match(r"^\d+:\d{2}", r["time"]) or r["name"] in seen:
            continue
        seen.add(r["name"])
        results.append({"name": r["name"], "time": r["time"], "place": int(place_str), "division": division_name,
                        "divisionSequence": sequence, "raceStartTime": start_time})
    return results


def _parse_row(cells: list, division_name: str, sequence: int, start_time: str | None) -> dict | None:
    """
    Parse cells into a result dict.
    Expected column order: Place | Name | Team | [Grade] | Time
    Time is identified by matching M:SS or H:MM:SS pattern.
    """
    try:
        texts = [c.inner_text().strip() for c in cells]

        place_str = re.sub(r"[^0-9]", "", texts[0].split("\n")[0])
        if not place_str:
            return None
        place = int(place_str)

        name = texts[1].split("\n")[0].strip()
        if not name:
            return None

        time_val = None
        for t in reversed(texts):
            if re.match(r"^\d+:\d{2}", t):
                time_val = t.strip()
                break

        if not time_val:
            return None

        return {"name": name, "time": time_val, "place": place, "division": division_name, "divisionSequence": sequence, "raceStartTime": start_time}

    except (ValueError, IndexError):
        return None


def scrape_meet(meet_url: str, debug: bool = False) -> list:
    """
    Full scrape: collect race URLs from the events page, then visit each
    race page and aggregate Ursuline Academy results.
    """
    all_results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=not debug)
        page = browser.new_page()

        races = collect_race_urls(page, meet_url, debug)

        if not races:
            print("\nERROR: No race links found on the events page.")
            print("Run with --debug to dump debug_events_page.html and inspect the HTML structure.")
            browser.close()
            return []

        print(f"\nVisiting {len(races)} race page(s) ...\n")

        for sequence, (race_url, division, start_time) in enumerate(races, start=1):
            print(f"  [{sequence}] {division}" + (f" ({start_time})" if start_time else ""))
            results = scrape_race_page(page, race_url, division, sequence, start_time, debug)
            ua_results = [r for r in results]  # already filtered to UA by scrape_race_page
            if ua_results:
                for r in ua_results:
                    print(f"    {r['place']:>3}. {r['name']:<30} {r['time']}")
                all_results.extend(ua_results)
            else:
                print("    (no Ursuline Academy results)")

        browser.close()

    return all_results


# ── Main ──────────────────────────────────────────────────────────────────────

def main(meet_url: str, meet_name: str, season: str, dry_run: bool, debug: bool):
    print("=" * 60)
    print(f"Meet:    {meet_name}")
    print(f"Season:  {season}")
    print(f"Mode:    {'DRY RUN' if dry_run else 'LIVE'}")
    print("=" * 60)

    print("\nFetching roster from backend ...")
    roster = fetch_roster(BASE_API_URL)
    print(f"Loaded {len(roster)} runners\n")

    scraped = scrape_meet(meet_url, debug=debug)

    print(f"\n{'='*60}")
    print(f"Scraped {len(scraped)} Ursuline Academy result(s)")

    if not scraped:
        print("Nothing to post.")
        return

    # Try to resolve scraped races to the coach's pre-configured divisions by relative order
    # (see build_division_rank_map) rather than leaving it to the backend's absolute start-time
    # matching, which breaks if the meet ran noticeably earlier/later than configured.
    division_rank_map = {}
    meet_id = fetch_meet_id(BASE_API_URL, meet_name, season)
    if meet_id is not None:
        divisions = fetch_divisions(BASE_API_URL, meet_id)
        division_rank_map = build_division_rank_map(scraped, divisions)
        if division_rank_map:
            print("\nMatched races to your configured divisions by order:")
            for race_seq, division_id in division_rank_map.items():
                division = next(d for d in divisions if d["id"] == division_id)
                race_label = next(r["division"] for r in scraped if r["divisionSequence"] == race_seq)
                print(f"  Race [{race_seq}] {race_label}  →  \"{division['name']}\" (id={division_id})")
        elif divisions:
            print(f"\n⚠  {len(divisions)} division(s) configured but {len({r['divisionSequence'] for r in scraped})} "
                  f"distinct race(s) scraped -- counts don't match, so results will fall back to the backend's "
                  f"own start-time/sequence/name matching (less reliable). Review division assignments after posting.")
    else:
        print(f"\n⚠  Could not find meet id for '{meet_name}' ({season}) -- results will fall back to the "
              f"backend's own start-time/sequence/name matching for divisions.")

    print("\nPosting to backend ...")
    unmatched = []
    created = []

    for r in scraped:
        runner_id = find_runner_id(r["name"], roster)

        if runner_id is None:
            print(f"  [UNMATCHED] {r['name']} ({r['division']})")
            unmatched.append(r)
            continue

        payload = {
            "season": season,
            "runnerId": runner_id,
            "meetName": meet_name,
            "time": r["time"],
            "place": r["place"],
            "passesLastMile": 0,
            "passesSecondMile": 0,
            "skullsEarned": 0,
            "mileOneSplit": "",
            "mileTwoSplit": "",
            "mileThreeSplit": "",
            "divisionSequence": r["divisionSequence"],
            "divisionLabel": r["division"],
            "raceStartTime": r["raceStartTime"]
        }
        if r["divisionSequence"] in division_rank_map:
            payload["divisionId"] = division_rank_map[r["divisionSequence"]]

        if dry_run:
            time_note = f" @ {r['raceStartTime']}" if r["raceStartTime"] else ""
            resolved_note = f"  →  \"{payload['divisionId']}\"" if "divisionId" in payload else "  (unresolved, backend will guess)"
            print(f"  [DRY RUN] {r['name']:<30} place={r['place']:<4} time={r['time']}  division=[{r['divisionSequence']}] {r['division']}{time_note}{resolved_note}")
        else:
            try:
                post_meet_result(BASE_API_URL, payload)
                print(f"  [CREATED]  {r['name']:<30} place={r['place']:<4} time={r['time']}")
                created.append(r)
            except requests.HTTPError as e:
                print(f"  [ERROR]    {r['name']} — {e}")

    print(f"\n{'='*60}")
    if dry_run:
        print(f"Dry run complete. Would post {len(scraped) - len(unmatched)} result(s).")
    else:
        print(f"Done. Created {len(created)} result(s).")

    if unmatched:
        print(f"\n⚠  {len(unmatched)} runner(s) could not be matched to the roster:")
        for r in unmatched:
            print(f"   '{r['name']}' ({r['division']})")
        print("\nTip: Check name spelling vs backend roster, or add the runner first.")


if __name__ == "__main__":
    args = sys.argv[1:]
    dry_run = "--dry-run" in args
    debug = "--debug" in args
    args = [a for a in args if not a.startswith("--")]

    if len(args) < 3:
        print("Usage: python scrape_meet_results.py <meet_url> <meet_name> <season> [--dry-run] [--debug]")
        print()
        print("Examples:")
        print('  python scrape_meet_results.py \\')
        print('    "https://finishtiming.trackscoreboard.com/meets/583161/events" \\')
        print('    "Troy Invitational" 2024 --dry-run')
        sys.exit(1)

    main(
        meet_url=args[0],
        meet_name=args[1],
        season=args[2],
        dry_run=dry_run,
        debug=debug,
    )