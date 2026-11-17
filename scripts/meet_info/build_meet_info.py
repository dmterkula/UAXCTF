#!/usr/bin/env python3
"""Build a pre-meet info sheet (PDF + editable DOCX) from a JSON spec.

Usage: build_meet_info.py specs/<meet>.json [--out out/]

Times for arrival / warm-up / line-up are computed from each race's start time and
the offsets (minutes before race), so they can't drift out of sync with the race time.
"""
import argparse, html, json, subprocess, sys
from datetime import datetime, timedelta
from pathlib import Path

CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
DEFAULTS = {
    "arrive_min": 85,       # arrival, minutes before race
    "warmup_min": 60,       # begin warm-up with a coach
    "lineup_min": 15,       # lined up in starting box
    "bring": ["Training shoes", "Racing shoes", "Uniform", "Water", "Healthy snacks",
              "Outer wear", "Watch the weather and prepare accordingly"],
    "after_race": "Team recap and Pride, proceed to Team Tent, change shoes, and cooldown.",
    "arrive_text": "Find UA tent/meeting area and receive bib number and chip. Use the bathroom.",
    "team_cheer": False,    # add "Cheer on the rest of the team!" after race
}


def parse_time(s):
    return datetime.strptime(s.strip().lower().replace(".", ""), "%I:%M %p")


def fmt(t):
    return t.strftime("%-I:%M %p").lower()


def race_times(race, spec):
    start = parse_time(race["time"])
    g = lambda k: race.get(k, spec.get(k, DEFAULTS[k]))
    return {
        "start": fmt(start),
        "arrive": fmt(start - timedelta(minutes=g("arrive_min"))),
        "warmup": fmt(start - timedelta(minutes=g("warmup_min"))),
        "lineup": fmt(start - timedelta(minutes=g("lineup_min"))),
    }


def build_schedule(spec):
    """Return list of dicts: heading + ordered (bold_text, plain_text, underline_word) lines."""
    out = []
    for r in spec["races"]:
        t = race_times(r, spec)
        size = f" (Expected Race Size: {r['expected_size']})" if r.get("expected_size") else ""
        lines = [
            (f"{t['arrive']} - arrival time.", r.get("arrive_note", "") , spec.get("arrive_text", DEFAULTS["arrive_text"])),
            (f"{t['warmup']} -", "Begin warm-up with a coach", ""),
            (f"{t['lineup']} - Lined up in starting box # for strides and final drills, race starts {t['start']}", "", ""),
        ]
        after = spec.get("after_race", DEFAULTS["after_race"])
        if spec.get("team_cheer", DEFAULTS["team_cheer"]):
            after = "Cheer on the rest of the team! " + after
        lines.append(("Immediately after the race –", after, ""))
        if r.get("after_extra"):
            lines.append(("", r["after_extra"], ""))
        out.append({"heading": f"{r['name']} – {t['start']} race time{size}", "lines": lines})
    return out


def columns(race):
    ath = race["athletes"]
    if len(ath) > 16:
        h = (len(ath) + 1) // 2
        return [ath[:h], ath[h:]]
    return [ath]


def esc(s):
    return html.escape(s)


def to_html(spec):
    d = spec
    bring = d.get("bring", DEFAULTS["bring"])
    sched = build_schedule(d)
    pending = any(a.endswith("*") for r in d["races"] for a in r["athletes"])
    p = []
    p.append(f"<h1>{esc(d['venue'])}</h1>")
    p.append(f"<div class='c'><b>{esc(d['address'])}</b></div>")
    if d.get("address_note"):
        p.append(f"<div class='c note'>{esc(d['address_note'])}</div>")
    p.append(f"<h2>{esc(d['date'])}</h2>")

    # Athlete tables: one column-group per race
    cells = []
    for r in d["races"]:
        cols = columns(r)
        for i, col in enumerate(cols):
            n = len(r["athletes"])
            hdr = f"{esc(r['time'])} - {esc(r['name'])}" + (f"<br>({esc(r.get('count_note', ''))})" if r.get("count_note") else "")
            cells.append((hdr, col))
    maxrows = max(len(c) for _, c in cells)
    p.append("<table class='ath'><tr>" + "".join(f"<th>{h}</th>" for h, _ in cells) + "</tr>")
    for i in range(maxrows):
        p.append("<tr>" + "".join(f"<td>{esc(c[i]) if i < len(c) else '&nbsp;'}</td>" for _, c in cells) + "</tr>")
    p.append("</table>")
    if d.get("not_running"):
        p.append(f"<p class='b'>Not running ({len(d['not_running'])}) – {esc(', '.join(d['not_running']))}</p>")
    if pending:
        p.append("<p class='b'>*Pending clearance/coaches decision</p>")
    p.append("<ul class='races'>" + "".join(
        f"<li><b>{esc(r['time'])} - {esc(r['name'])}</b></li>" for r in d["races"]) + "</ul>")

    p.append("<div class='pb'></div><p class='b'>Schedule of Events</p>")
    for s in sched:
        p.append(f"<p class='hd'><b><u>{esc(s['heading'])}</u></b></p><div class='sched'>")
        for bold, plain, extra in s["lines"]:
            if bold or plain or extra:
                p.append(f"<div>{'<b>'+esc(bold)+'</b> ' if bold else ''}{esc(plain)} {esc(extra)}</div>")
        p.append("</div>")
    p.append("<h3>What to Bring</h3><ul>" + "".join(f"<li>{esc(b)}</li>" for b in bring) + "</ul>")
    if d.get("results_site"):
        p.append(f"<p><b>RESULTS:</b> {esc(d['results_site'])}</p>")
    if d.get("awards_time"):
        p.append(f"<p class='b'>Awards at {esc(d['awards_time'])}</p>")
    if d.get("awards"):
        p.append("<p class='b'>Awards</p><ul class='plain'>" + "".join(f"<li>{esc(a)}</li>" for a in d["awards"]) + "</ul>")
    if d.get("notes"):
        p.append("<p class='b'>Miscellaneous notes:</p><ul>" + "".join(f"<li>{esc(n)}</li>" for n in d["notes"]) + "</ul>")
    if d.get("course_map"):
        p.append(f"<div class='pb'></div><p class='b'><u>Course Map</u></p><img class='map' src='{Path(d['course_map']).resolve().as_uri()}'>")
    css = """
    @page{size:Letter;margin:0.6in}
    body{font-family:Arial,Helvetica,sans-serif;font-size:11pt;color:#000}
    h1{font-size:16pt;text-align:center;margin:0} h2{font-size:13pt;text-align:center;margin:14px 0}
    h3{font-size:12pt;margin:14px 0 4px} .c{text-align:center} .note{font-size:9.5pt;font-weight:bold}
    table.ath{border-collapse:collapse;width:100%;margin:8px 0}
    table.ath th,table.ath td{border:1px solid #000;padding:3px 6px;font-size:10.5pt;text-align:left}
    table.ath th{text-align:center}
    .b{font-weight:bold} .pb{page-break-before:always} .hd{margin:16px 0 4px}
    .sched div{margin:2px 0} ul{margin:4px 0} ul.plain{list-style:none;padding-left:0}
    ul.races{margin-top:12px} img.map{max-width:100%;max-height:8.5in}
    """
    return f"<!doctype html><meta charset='utf-8'><title>{esc(d['venue'])}</title><style>{css}</style>" + "".join(p)


def to_docx(spec, path):
    from docx import Document
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.shared import Pt
    d = spec
    doc = Document()
    doc.styles["Normal"].font.name = "Arial"; doc.styles["Normal"].font.size = Pt(11)
    def center(text, bold=True, size=None):
        p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(text); r.bold = bold
        if size: r.font.size = Pt(size)
    center(d["venue"], size=16); center(d["address"])
    if d.get("address_note"): center(d["address_note"], size=9.5)
    center(d["date"], size=13)
    cells = []
    for r in d["races"]:
        for col in columns(r):
            cells.append((f"{r['time']} - {r['name']}", col))
    tbl = doc.add_table(rows=1 + max(len(c) for _, c in cells), cols=len(cells)); tbl.style = "Table Grid"
    for j, (h, col) in enumerate(cells):
        tbl.cell(0, j).text = h
        for run in tbl.cell(0, j).paragraphs[0].runs: run.bold = True
        for i, a in enumerate(col): tbl.cell(i + 1, j).text = a
    if d.get("not_running"):
        p = doc.add_paragraph(); p.add_run(f"Not running ({len(d['not_running'])}) – {', '.join(d['not_running'])}").bold = True
    if any(a.endswith("*") for r in d["races"] for a in r["athletes"]):
        doc.add_paragraph().add_run("*Pending clearance/coaches decision").bold = True
    doc.add_paragraph().add_run("Schedule of Events").bold = True
    for s in build_schedule(d):
        p = doc.add_paragraph(); r = p.add_run(s["heading"]); r.bold = True; r.underline = True
        for bold, plain, extra in s["lines"]:
            if not (bold or plain or extra): continue
            p = doc.add_paragraph(); p.paragraph_format.space_after = Pt(0)
            if bold: p.add_run(bold + " ").bold = True
            p.add_run(f"{plain} {extra}".strip())
    doc.add_paragraph().add_run("What to Bring").bold = True
    for b in d.get("bring", DEFAULTS["bring"]): doc.add_paragraph(b, style="List Bullet")
    if d.get("results_site"): 
        p = doc.add_paragraph(); p.add_run("RESULTS: ").bold = True; p.add_run(d["results_site"])
    if d.get("awards_time"): doc.add_paragraph().add_run(f"Awards at {d['awards_time']}").bold = True
    if d.get("awards"):
        doc.add_paragraph().add_run("Awards").bold = True
        for a in d["awards"]: doc.add_paragraph(a, style="List Bullet")
    if d.get("notes"):
        doc.add_paragraph().add_run("Miscellaneous notes:").bold = True
        for n in d["notes"]: doc.add_paragraph(n, style="List Bullet")
    if d.get("course_map"):
        from docx.shared import Inches
        doc.add_page_break(); doc.add_paragraph().add_run("Course Map").bold = True
        doc.add_picture(d["course_map"], width=Inches(6))
    doc.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("spec"); ap.add_argument("--out", default=str(Path(__file__).parent / "out"))
    a = ap.parse_args()
    spec = json.loads(Path(a.spec).read_text())
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    stem = Path(a.spec).stem
    h = out / f"{stem}.html"; h.write_text(to_html(spec))
    pdf = out / f"{stem}.pdf"
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                    f"--print-to-pdf={pdf}", h.as_uri()], check=True, capture_output=True)
    to_docx(spec, out / f"{stem}.docx")
    print(f"Wrote {pdf}\nWrote {out / (stem + '.docx')}")


if __name__ == "__main__":
    sys.exit(main())
