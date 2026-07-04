#!/usr/bin/env python3
"""
Athlete PR Scraper
Scrapes Ursuline Academy roster from MileSplit and builds a CSV of personal records.

This script:
  1. Fetches the team roster from oh.milesplit.com (JSON-LD structured data)
  2. Visits each athlete's profile page and scrapes their PRs
  3. Writes results to a CSV file

PRs are server-rendered on MileSplit and don't require a login.

Setup (one time):
    cd /path/to/UAXCTF
    source scripts/venv/bin/activate
    pip install requests beautifulsoup4 lxml

Usage:
    python scripts/scrape_athlete_prs.py [--output FILE] [--debug]

Output:
    Writes athlete_prs.csv (or custom path) with columns:
        athlete, event, season_type, pr
"""

import sys
import re
import os
import csv
import json
import time
import requests
from bs4 import BeautifulSoup

TEAM_URL = "https://oh.milesplit.com/teams/9403-ursuline-academy/roster"
HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
}

# Normalize event names to a canonical form used in the meet summary script
EVENT_ALIASES = {
    "5000 Meter Run": "5000m",
    "Three Mile Run": "3 Mile",
    "5000 Meters": "5000m",
    "3200 Meter Run": "3200m",
    "3200 Meters": "3200m",
    "1600 Meter Run": "1600m",
    "1600 Meters": "1600m",
    "800 Meter Run": "800m",
    "800 Meters": "800m",
    "400 Meter Dash": "400m",
    "400 Meters": "400m",
    "300 Meter Hurdles": "300m Hurdles",
    "300m Hurdles": "300m Hurdles",
    "200 Meter Dash": "200m",
    "200 Meters": "200m",
    "100 Meter Dash": "100m",
    "100 Meters": "100m",
    "100 Meter Hurdles": "100m Hurdles",
    "100m Hurdles": "100m Hurdles",
    "4x800 Meter Relay": "4x800m Relay",
    "4x400 Meter Relay": "4x400m Relay",
    "4x200 Meter Relay": "4x200m Relay",
    "4x100 Meter Relay": "4x100m Relay",
    "Shot Put": "Shot Put",
    "Discus": "Discus",
    "Discus Throw": "Discus",
    "Long Jump": "Long Jump",
    "High Jump": "High Jump",
    "Pole Vault": "Pole Vault",
    "Triple Jump": "Triple Jump",
    "Javelin": "Javelin",
    "Javelin Throw": "Javelin",
}


def normalize_event(event_name: str) -> str:
    """Normalize event name to canonical form."""
    return EVENT_ALIASES.get(event_name, event_name)


def fetch_roster(debug: bool) -> list[dict]:
    """
    Fetch the Ursuline Academy roster from MileSplit.
    Returns list of {name, url, gender, class_year} dicts.
    """
    print("Fetching roster...")
    resp = requests.get(TEAM_URL, headers=HEADERS, timeout=15)
    resp.raise_for_status()

    soup = BeautifulSoup(resp.text, "lxml")

    # Extract from JSON-LD structured data (most reliable)
    athletes = []
    for script in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(script.string)
        except (json.JSONDecodeError, TypeError):
            continue

        if "athlete" not in data:
            continue

        for a in data["athlete"]:
            name = a.get("name", "")
            url = a.get("url", "")
            gender = a.get("gender", "")
            if name and url:
                athletes.append({
                    "name": name,
                    "url": url,
                    "gender": gender,
                })

    print(f"Found {len(athletes)} athletes on roster")

    if debug and athletes:
        for a in athletes[:5]:
            print(f"  {a['name']} -> {a['url']}")
        if len(athletes) > 5:
            print(f"  ... and {len(athletes) - 5} more")

    return athletes


def scrape_athlete_prs(athlete: dict, debug: bool) -> list[dict]:
    """
    Scrape PRs from an athlete's MileSplit profile page.
    Returns list of {event, season_type, pr} dicts.
    """
    url = athlete["url"]
    if not url.endswith("/stats"):
        url = url.rstrip("/") + "/stats"

    try:
        resp = requests.get(url, headers=HEADERS, timeout=15)
        resp.raise_for_status()
    except requests.RequestException as e:
        if debug:
            print(f"    [DEBUG] Failed to fetch {url}: {e}")
        return []

    soup = BeautifulSoup(resp.text, "lxml")

    prs = []
    seen = set()

    for box in soup.find_all("div", class_="record-box"):
        # Only PR boxes (contain the personal-record icon)
        has_pr = box.find("img", src=lambda x: x and "personal-record" in x) is not None
        if not has_pr:
            continue

        table = box.find("table", class_="table")
        if not table:
            continue

        current_season = ""
        for tr in table.find_all("tr"):
            season_td = tr.find("td", class_="season")
            if season_td:
                current_season = season_td.get_text(strip=True)
                continue

            event_td = tr.find("td", class_="event")
            time_td = tr.find("td", class_="time")
            if event_td and time_td:
                event = event_td.get_text(strip=True)
                mark = time_td.get_text(strip=True)
                key = (current_season, event, mark)
                if key not in seen:
                    seen.add(key)
                    prs.append({
                        "event": normalize_event(event),
                        "season_type": current_season,
                        "pr": mark,
                    })

    return prs


def write_csv(all_prs: list[dict], output_path: str):
    """Write all PRs to a CSV file."""
    with open(output_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["athlete", "event", "season_type", "pr"])
        writer.writeheader()
        writer.writerows(all_prs)

    print(f"\nCSV written to: {os.path.abspath(output_path)}")
    print(f"Total entries: {len(all_prs)}")


def main(output_path: str, debug: bool):
    athletes = fetch_roster(debug)

    if not athletes:
        print("ERROR: No athletes found on roster.")
        sys.exit(1)

    all_prs = []
    athletes_with_prs = 0

    for i, athlete in enumerate(athletes):
        name = athlete["name"]
        print(f"  [{i+1}/{len(athletes)}] {name} ...", end="", flush=True)

        prs = scrape_athlete_prs(athlete, debug)
        print(f" {len(prs)} PR(s)")

        if debug and prs:
            for pr in prs:
                print(f"    {pr['season_type']} {pr['event']}: {pr['pr']}")

        if prs:
            athletes_with_prs += 1
            for pr in prs:
                all_prs.append({
                    "athlete": name,
                    "event": pr["event"],
                    "season_type": pr["season_type"],
                    "pr": pr["pr"],
                })

        # Be polite — don't hammer the server
        time.sleep(0.5)

    print(f"\n{athletes_with_prs}/{len(athletes)} athletes have PRs on MileSplit")
    write_csv(all_prs, output_path)


if __name__ == "__main__":
    args = sys.argv[1:]
    debug = "--debug" in args
    args = [a for a in args if a != "--debug"]

    # Parse --output flag
    output_path = "athlete_prs.csv"
    filtered_args = []
    i = 0
    while i < len(args):
        if args[i] == "--output" and i + 1 < len(args):
            output_path = args[i + 1]
            i += 2
        else:
            filtered_args.append(args[i])
            i += 1

    main(output_path=output_path, debug=debug)