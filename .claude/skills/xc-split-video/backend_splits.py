#!/usr/bin/env python3
"""List UA runners' backend splits for a meet, to compare against video-derived results.

Usage: backend_splits.py "<Meet Name>" <season> [--mile 1|2]

For each runner prints: cumulative split at the chosen mile, name, division, final place,
and position at that mile reconstructed from passes:
    pos_mile2 = place + passesLastMile
    pos_mile1 = place + passesLastMile + passesSecondMile
Note: place is the DIVISION place, while passes are counted against everyone seen on course,
so reconstructed positions are approximate when divisions run together.
"""
import argparse

import requests

BASE = "http://ec2-3-138-67-225.us-east-2.compute.amazonaws.com"


def secs(t):
    m, s = t.split(":")
    return int(m) * 60 + float(s)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("meet")
    ap.add_argument("season")
    ap.add_argument("--mile", type=int, default=2, choices=[1, 2])
    a = ap.parse_args()
    rows = requests.get(f"{BASE}/xc/meetResults",
                        params={"filter.meetName": a.meet, "filter.season": a.season, "page.size": 500},
                        timeout=20).json()
    out = []
    for e in rows:
        p = e["performance"]
        m1, m2 = p.get("mileOneSplit"), p.get("mileTwoSplit")
        if not m1 or (a.mile == 2 and not m2):
            continue
        cum = secs(m1) + (secs(m2) if a.mile == 2 else 0)
        last = p.get("passesLastMile") or 0
        second = p.get("passesSecondMile") or 0
        pos = p["place"] + last + (second if a.mile == 1 else 0)
        out.append((cum, e["runner"]["name"], p["divisionName"], p["place"], pos))
    print(f"{'split':>6}  {'runner':24} {'division':9} {'final':>5} {'pos@mile' + str(a.mile):>10}")
    for cum, name, div, place, pos in sorted(out):
        print(f"{int(cum // 60)}:{int(cum % 60):02d}  {name:24} {div:9} {place:>5} {pos:>10}")


if __name__ == "__main__":
    main()
