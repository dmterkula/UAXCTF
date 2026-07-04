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

        # Skip the events listing page itself
        if not re.search(r"/events/\d+", href):
            continue

        race_url = href if href.startswith("http") else base_origin + href
        if race_url in seen:
            continue
        seen.add(race_url)

        # Try to grab a division label from the surrounding context
        division = _get_division_label(link, meet_url)
        races.append((race_url, division))
        print(f"  Found: {division}  →  {race_url}")

    return races


def _get_division_label(link_el, fallback: str) -> str:
    """
    Try to extract the event/division name that surrounds a race link.
    The page shows division headings above each Race N link, so we walk
    up the DOM looking for a nearby text heading.
    """
    try:
        # Ask the browser to walk up and find a nearby heading or label
        label = link_el.evaluate("""el => {
            // Try closest card/list item first
            const container = el.closest('mat-card, mat-list-item, .event-item, li, tr');
            if (container) return container.innerText.split('\\n')[0].trim();

            // Walk up two levels and grab first line of text
            const parent = el.parentElement?.parentElement;
            if (parent) return parent.innerText.split('\\n')[0].trim();

            return '';
        }""")
        return label.strip() if label and label.strip() else link_el.inner_text().strip()
    except Exception:
        return link_el.inner_text().strip() or fallback


def scrape_race_page(page, race_url: str, division_name: str, debug: bool) -> list:
    """
    Phase 2: Navigate to a single race results page and return all
    Ursuline Academy results as {name, time, place, division} dicts.
    """
    page.goto(race_url, wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(PAGE_LOAD_WAIT_MS)

    if debug:
        race_id = race_url.rstrip("/").split("/")[-1]
        with open(f"debug_race_{race_id}.html", "w") as f:
            f.write(page.content())
        print(f"  [DEBUG] Wrote debug_race_{race_id}.html")

    # Try to get a better division name from the page heading
    try:
        heading = page.query_selector("h1, h2, h3, mat-toolbar span, .event-title, .race-name")
        if heading:
            text = heading.inner_text().strip()
            if text:
                division_name = text
    except Exception:
        pass

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

        result = _parse_row(cells, division_name)
        if result:
            results.append(result)

    return results


def _parse_row(cells: list, division_name: str) -> dict | None:
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

        return {"name": name, "time": time_val, "place": place, "division": division_name}

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

        for race_url, division in races:
            print(f"  {division}")
            results = scrape_race_page(page, race_url, division, debug)
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
            "mileThreeSplit": ""
        }

        if dry_run:
            print(f"  [DRY RUN] {r['name']:<30} place={r['place']:<4} time={r['time']}")
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