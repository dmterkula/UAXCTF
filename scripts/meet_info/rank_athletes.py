#!/usr/bin/env python3
"""Rank active UA XC athletes by 2026 season best (from the UAXCTF API) to seed race lineups.

Usage: rank_athletes.py [--season 2026]
Prints a ranked table plus 'Name*' candidates. Athletes with no race yet are listed last.
"""
import argparse, json, urllib.request

BASE = "http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com"


def secs(t):
    m, s = t.split(":")
    return int(m) * 60 + float(s)


def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--season", default="2026")
    a = ap.parse_args()
    url = f"{BASE}/xc/seasonBests/all?filter.season={a.season}&adjust.forDistance=true"
    data = json.load(urllib.request.urlopen(url, timeout=30))["seasonBests"]
    rows = []
    for d in data:
        r = d["runner"]
        if not (r.get("active") and r.get("doesXc")):
            continue
        best = min(d["result"], key=lambda x: secs(x["time"])) if d["result"] else None
        rows.append((r["name"], r["graduatingClass"], best["time"] if best else None, best["meetName"] if best else ""))
    rows.sort(key=lambda x: (x[2] is None, secs(x[2]) if x[2] else 0))
    for i, (n, gc, t, m) in enumerate(rows, 1):
        print(f"{i:>2}. {n:<22} {gc}  {t or '--':>8}  {m}")


if __name__ == "__main__":
    main()
