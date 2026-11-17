#!/usr/bin/env python3
"""
XC Meet Summary Generator
Calls /xc/meetSummary and generates a plain-text recap or an HTML file
you can open in a browser, select-all, copy, and paste into an email.

Setup (one time):
    cd /path/to/UAXCTF
    python3 -m venv scripts/venv
    source scripts/venv/bin/activate
    pip install requests

Each session:
    source scripts/venv/bin/activate

Usage:
    python scripts/meet_summary.py <meet_name> <season> [--html]

Examples:
    # Print plain text to terminal:
    python scripts/meet_summary.py "Troy" 2024

    # Generate email-ready HTML (open in browser → Cmd+A → Cmd+C → paste):
    python scripts/meet_summary.py "Troy" 2024 --html

Output (--html):
    Writes meet_summary_Troy_2024.html to the current directory.
"""

import sys
import re
import os
import requests

BASE_API_URL = "http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com"
PASSES_THRESHOLD = 10


# ── Helpers ───────────────────────────────────────────────────────────────────

def ordinal(n: int) -> str:
    if 11 <= (n % 100) <= 13:
        return "th"
    return {1: "st", 2: "nd", 3: "rd"}.get(n % 10, "th")


def fmt_time(t: str) -> str:
    t = t.strip()
    if "." in t:
        t = t.rstrip("0").rstrip(".")
    return t


def time_to_seconds(t: str) -> float:
    minutes, seconds = t.strip().split(":")
    return int(minutes) * 60 + float(seconds)


def fmt_improvement(diff: str) -> str:
    return diff.lstrip("-")


def fmt_improvement_natural(diff: str) -> str:
    """Like fmt_improvement, but renders sub-minute durations as a plain truncated
    second count ("19s") instead of "0:19.6" -- reads more naturally in a sentence."""
    diff = fmt_improvement(diff)
    parts = diff.split(":")
    if len(parts) == 2 and int(parts[0]) == 0:
        return f"{int(float(parts[1]))}s"
    return diff


def is_improvement(diff: str) -> bool:
    if not diff or diff in ("", "0:00.0", "0:00"):
        return False
    return diff.startswith("-")


def is_year_over_year_improvement(prog: str) -> bool:
    return bool(prog) and not prog.startswith("-") and prog not in ("0:00", "0:00.0")


# ── API ───────────────────────────────────────────────────────────────────────

def fetch_summary(meet_name: str, season: str) -> dict:
    resp = requests.get(
        f"{BASE_API_URL}/xc/meetSummary",
        params={"filter.meet": meet_name, "season": season, "page.size": 500},
        timeout=15
    )
    resp.raise_for_status()
    return resp.json()


def fetch_meet_results(meet_name: str, season: str) -> list:
    """
    Full roster of runners who actually competed at this meet. The
    /xc/meetSummary endpoint only surfaces runners with a PR/SB/improvement
    callout, so it undercounts "N runners competed" and drops anyone without
    a notable result. This fills in the rest.
    """
    resp = requests.get(
        f"{BASE_API_URL}/xc/meetResults",
        params={"filter.meetName": meet_name, "filter.season": season, "page.size": 500},
        timeout=15
    )
    resp.raise_for_status()
    return resp.json()


def fetch_season_rank_counts(season: str, meet_results: list) -> dict:
    """
    {runner_name: number of fall races this season (before this meet) that were faster than
    this meet's time}. Used to call out a 2nd/3rd-best time when it wasn't a PR/SB. Only counts
    meets from Jul 1 through this meet's date, so spring/track-season entries don't count.
    """
    if not meet_results:
        return {}
    meet_date = meet_results[0]["performance"]["meetDate"]
    this_time = {e["runner"]["name"]: time_to_seconds(e["performance"]["time"]) for e in meet_results}
    meets = requests.get(f"{BASE_API_URL}/xc/meets/getBySeason", params={"season": season}, timeout=15).json()
    faster = {name: 0 for name in this_time}
    for m in meets:
        if not (f"{season}-07-01" <= m["date"] < meet_date):
            continue
        resp = requests.get(
            f"{BASE_API_URL}/xc/meetResults",
            params={"filter.meetName": m["name"], "filter.season": season, "page.size": 500},
            timeout=15
        )
        resp.raise_for_status()
        for e in resp.json():
            name = e["runner"]["name"]
            perf = e["performance"]
            if perf["meetDate"] == m["date"] and name in this_time and time_to_seconds(perf["time"]) < this_time[name]:
                faster[name] += 1
    return faster


# ── Data Extraction ───────────────────────────────────────────────────────────

def build_runner_map(data: dict, meet_results: list) -> dict:
    runners = {}

    for entry in meet_results:
        name = entry["runner"]["name"]
        perf = entry["performance"]
        runners[name] = {
            "name": name,
            "place": perf["place"],
            "time": perf["time"],
            "passes2": perf.get("passesSecondMile", 0),
            "passesL": perf.get("passesLastMile", 0),
            "is_sb": False,
            "is_pr": False,
            "sb_diff": "",
            "improved_from_last": None,
            "improved_from_last_year": None,
        }

    for sb in data.get("seasonBests", {}).get("seasonBests", []):
        name = sb["runner"]["name"]
        r = sb["result"][0]
        improved_upon = sb.get("improvedUpon", {})
        runners[name] = {
            "name": name,
            "place": r["place"],
            "time": r["time"],
            "passes2": r.get("passesSecondMile", 0),
            "passesL": r.get("passesLastMile", 0),
            "is_sb": True,
            "is_pr": False,
            "sb_diff": improved_upon.get("timeDifference", "") if improved_upon.get("meet") else "",
            "improved_from_last": None,
            "improved_from_last_year": None,
        }

    for pr in data.get("prs", {}).get("PRs", []):
        name = pr["runner"]["name"]
        if name in runners:
            runners[name]["is_pr"] = True
        else:
            r = pr["result"][0]
            improved_upon = pr.get("improvedUpon", {})
            runners[name] = {
                "name": name,
                "place": r["place"],
                "time": r["time"],
                "passes2": r.get("passesSecondMile", 0),
                "passesL": r.get("passesLastMile", 0),
                "is_sb": True,
                "is_pr": True,
                "sb_diff": improved_upon.get("timeDifference", "") if improved_upon.get("meet") else "",
                "improved_from_last": None,
                "improved_from_last_year": None,
            }

    improvement = data.get("summaryImprovementFromLastMeet", {})
    for r in improvement.get("faster", {}).get("improvementRateDTOs", []):
        name = r["runner"]["name"]
        rate = r.get("improvementRate", "")
        num_meets = r.get("numMeets", 1)
        if num_meets > 1 and is_improvement(rate):
            if name in runners:
                runners[name]["improved_from_last"] = fmt_improvement(rate)
            else:
                runners.setdefault(name, {
                    "name": name, "place": None, "time": None,
                    "passes2": 0, "passesL": 0,
                    "is_sb": False, "is_pr": False, "sb_diff": "",
                    "improved_from_last": None, "improved_from_last_year": None,
                })["improved_from_last"] = fmt_improvement(rate)

    for r in data.get("comparisonFromLastYear", {}).get("fasterProgression", {}).get("results", []):
        name = r["runner"]["name"]
        prog = r.get("progression", [""])[0]
        if is_year_over_year_improvement(prog):
            if name in runners:
                runners[name]["improved_from_last_year"] = prog
            else:
                perfs = r.get("performances", [])
                if len(perfs) >= 2:
                    cur = perfs[1]
                    runners[name] = {
                        "name": name,
                        "place": cur["place"],
                        "time": cur["time"],
                        "passes2": cur.get("passesSecondMile", 0),
                        "passesL": cur.get("passesLastMile", 0),
                        "is_sb": False, "is_pr": False, "sb_diff": "",
                        "improved_from_last": None,
                        "improved_from_last_year": prog,
                    }

    return runners


# ── Sentence Parts ────────────────────────────────────────────────────────────

def sentence_parts(r: dict):
    """
    Returns a dict of the logical pieces of a runner's sentence so both
    plain-text and HTML renderers can bold selectively.
    Keys: name, place_str, label (PR/SB/None), time, extras (list of strings)
    """
    name = r["name"]
    place = r.get("place")
    time = fmt_time(r["time"]) if r.get("time") else None

    if place is None or time is None:
        return {"raw": f"{name} competed at the meet."}

    suf = ordinal(place)
    place_str = f"{place}{suf}"
    label = "PR" if r["is_pr"] else ("SB" if r["is_sb"] else None)

    extras = []
    if label is None and r.get("season_rank") in (2, 3):
        extras.append(f"her {r['season_rank']}{ordinal(r['season_rank'])}-best time of the season")
    sb_clause_added = False
    if is_improvement(r.get("sb_diff", "")):
        record_kind = "PR" if r["is_pr"] else "SB"
        extras.append(f"a {fmt_improvement(r['sb_diff'])} improvement over her previous {record_kind}")
        sb_clause_added = True

    # If the previous record was set at the last meet, "improved from last meet" is the same
    # number as the improvement clause above -- skip it rather than stating it twice.
    same_as_sb_improvement = (
        sb_clause_added
        and r.get("improved_from_last")
        and abs(time_to_seconds(fmt_improvement(r["sb_diff"])) - time_to_seconds(fmt_improvement(r["improved_from_last"]))) < 0.15
    )
    if r.get("improved_from_last") and not same_as_sb_improvement:
        extras.append(f"she improved {fmt_improvement_natural(r['improved_from_last'])} from last meet")
    if r.get("improved_from_last_year"):
        extras.append(f"she improved {fmt_improvement_natural(r['improved_from_last_year'])} from last year at this meet")
    p2 = r.get("passes2", 0) or 0
    pL = r.get("passesL", 0) or 0
    if p2 > PASSES_THRESHOLD:
        extras.append(f"passing {p2} runners in the 2nd mile")
    if pL > PASSES_THRESHOLD:
        extras.append(f"passing {pL} runners in the last mile")

    return {
        "name": name,
        "place_str": place_str,
        "label": label,
        "time": time,
        "extras": extras,
    }


def join_extras(base: str, extras: list) -> str:
    """Joins the extra clauses onto the base sentence, Oxford-and style
    ("X, Y, and Z") so trailing full-clause extras like "she improved ..."
    read as a natural continuation rather than a bare comma-list."""
    if not extras:
        return base + "."
    if len(extras) == 1:
        return base + ", " + extras[0] + "."
    return base + ", " + ", ".join(extras[:-1]) + ", and " + extras[-1] + "."


def build_plain_sentence(r: dict) -> str:
    parts = sentence_parts(r)
    if "raw" in parts:
        return parts["raw"]

    name = parts["name"]
    place_str = parts["place_str"]
    label = parts["label"]
    time = parts["time"]
    extras = parts["extras"]

    if label:
        base = f"{name} finished {place_str} in a {label} time of {time}"
    else:
        base = f"{name} finished {place_str} with a time of {time}"

    return join_extras(base, extras)


def build_html_sentence(r: dict) -> str:
    parts = sentence_parts(r)
    if "raw" in parts:
        return f"<p>{parts['raw']}</p>"

    b = "<strong>{}</strong>".format

    name = b(parts["name"])
    place_str = b(parts["place_str"])
    label = parts["label"]
    time = b(parts["time"])
    extras = parts["extras"]

    if label:
        base = f"{name} finished {place_str} in a {b(label)} time of {time}"
    else:
        base = f"{name} finished {place_str} with a time of {time}"

    sentence = join_extras(base, extras)
    return f"<p>{sentence}</p>"


# ── Output ────────────────────────────────────────────────────────────────────

def group_by_division(sorted_runners, meet_results):
    """[(division_name, runners)] in order of each division's fastest time -- places are per race,
    so a single combined list reads wrong when a meet has e.g. Varsity/JV/Open races."""
    division = {e["runner"]["name"]: (e["performance"].get("divisionName") or "") for e in meet_results}
    groups = {}
    for r in sorted_runners:
        groups.setdefault(division.get(r["name"], ""), []).append(r)
    return list(groups.items())


def year_over_year_lines(data):
    comp = data.get("comparisonFromLastYear", {})
    faster = comp.get("fasterProgression", {}).get("results", [])
    slower = comp.get("slowerProgressions", {}).get("results", [])
    if not faster and not slower:
        return []
    lines = [f"{len(faster)} of {len(faster) + len(slower)} returners ran faster than at this meet last year."]
    gains = [time_to_seconds(r["progression"][0]) for r in faster if r.get("progression")]
    if gains:
        lines[0] = lines[0][:-1] + f" (median improvement: {fmt_improvement_natural(f'{int(sorted(gains)[len(gains)//2]//60)}:{sorted(gains)[len(gains)//2]%60:04.1f}')})."
    return lines


def print_plain(meet_name, season, runners, sorted_runners, data, meet_results=()):
    impr = data.get("summaryImprovementFromLastMeet", {})
    faster_count = impr.get("faster", {}).get("count", 0)
    slower_count = impr.get("slower", {}).get("count", 0)
    avg_diff = fmt_improvement(impr.get("averageDifference", ""))
    med_diff = fmt_improvement(impr.get("medianDifference", ""))
    sb_count = data.get("seasonBests", {}).get("count", 0)
    pr_count = data.get("prs", {}).get("count", 0)

    print()
    print("=" * 65)
    print(f"URSULINE ACADEMY — {meet_name} ({season})")
    print("=" * 65)
    print(f"{len(runners)} runners competed.")
    if faster_count or slower_count:
        print(f"{faster_count} ran faster than last week "
              f"(avg improvement: {avg_diff}, median: {med_diff}).")
        if slower_count:
            print(f"{slower_count} ran slower than last week.")
    for line in year_over_year_lines(data):
        print(line)
    print(f"{pr_count} PR(s).  {sb_count} Season Best(s).")
    print("-" * 65)
    groups = group_by_division(sorted_runners, meet_results)
    for division, group in groups:
        print()
        if len(groups) > 1 and division:
            print(division)
        for r in group:
            print(build_plain_sentence(r))
    print()


def write_html(meet_name, season, runners, sorted_runners, data, out_path, meet_results=()):
    impr = data.get("summaryImprovementFromLastMeet", {})
    faster_count = impr.get("faster", {}).get("count", 0)
    slower_count = impr.get("slower", {}).get("count", 0)
    avg_diff = fmt_improvement(impr.get("averageDifference", ""))
    med_diff = fmt_improvement(impr.get("medianDifference", ""))
    sb_count = data.get("seasonBests", {}).get("count", 0)
    pr_count = data.get("prs", {}).get("count", 0)

    header_lines = [
        f"<p><strong>URSULINE ACADEMY — {meet_name} ({season})</strong></p>",
        f"<p>{len(runners)} runners competed.</p>",
    ]
    if faster_count or slower_count:
        header_lines.append(
            f"<p>{faster_count} ran faster than last week "
            f"(avg improvement: {avg_diff}, median: {med_diff}).</p>"
        )
        if slower_count:
            header_lines.append(f"<p>{slower_count} ran slower than last week.</p>")
    for line in year_over_year_lines(data):
        header_lines.append(f"<p>{line}</p>")
    header_lines.append(f"<p>{pr_count} PR(s). {sb_count} Season Best(s).</p>")
    header_lines.append("<hr>")

    groups = group_by_division(sorted_runners, meet_results)
    body_lines = []
    for division, group in groups:
        if len(groups) > 1 and division:
            body_lines.append(f"<p style=\"margin-top:12px\"><strong><u>{division}</u></strong></p>")
        body_lines += [build_html_sentence(r) for r in group]

    html = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body {{
      font-family: Arial, sans-serif;
      font-size: 14px;
      line-height: 1.6;
      max-width: 800px;
      margin: 40px auto;
      padding: 0 20px;
    }}
    p {{ margin: 4px 0; }}
    hr {{ margin: 12px 0; border: none; border-top: 1px solid #ccc; }}
  </style>
</head>
<body>
{"".join(header_lines)}
{"".join(body_lines)}
</body>
</html>"""

    with open(out_path, "w") as f:
        f.write(html)

    print(f"HTML written to: {os.path.abspath(out_path)}")
    print("Open it in a browser, Cmd+A, Cmd+C, then paste into your email.")


# ── Main ──────────────────────────────────────────────────────────────────────

def main(meet_name: str, season: str, html_mode: bool):
    print(f"Fetching summary for '{meet_name}' ({season}) ...")
    data = fetch_summary(meet_name, season)
    meet_results = fetch_meet_results(meet_name, season)

    runners = build_runner_map(data, meet_results)
    for name, faster_count in fetch_season_rank_counts(season, meet_results).items():
        if name in runners:
            runners[name]["season_rank"] = faster_count + 1
    sorted_runners = sorted(
        runners.values(),
        key=lambda r: (r.get("time") is None, time_to_seconds(r["time"]) if r.get("time") else 0)
    )

    if html_mode:
        out_path = f"meet_summary_{meet_name.replace(' ', '_')}_{season}.html"
        write_html(meet_name, season, runners, sorted_runners, data, out_path, meet_results)
    else:
        print_plain(meet_name, season, runners, sorted_runners, data, meet_results)


if __name__ == "__main__":
    args = sys.argv[1:]
    html_mode = "--html" in args
    args = [a for a in args if not a.startswith("--")]

    if len(args) < 2:
        print("Usage: python meet_summary.py <meet_name> <season> [--html]")
        print('Example: python meet_summary.py "Troy" 2024 --html')
        sys.exit(1)

    main(meet_name=args[0], season=args[1], html_mode=html_mode)