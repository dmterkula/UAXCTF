#!/usr/bin/env python3
"""App-shaped workout payloads + offline validator + notification-safe API caller.

Source of truth: iphone-uaxc WorkoutComponentFormViewModel / WorkoutFormView (the form ALWAYS sends these shapes).
  - duration / recovery / setRecovery are always "mm:ss" strings (never null; "00:00" when unused)
  - recoveryType is always set ("Standing" default); recoveryDistance is always an Int (0 default, never null)
  - percent is always an Int (100 default; slider 50-120); sets is an Int; ratio is null only when 0
  - form refuses to save without: type, pace, targetDistance != 0, description, targetEvent != 0
  - targetEvent in {400,800,1600,3200,5000}; type in {Interval,Tempo,Progression}
  - pace in {SB, SB Avg, PR, Goal, Base Training Pace}; Active recovery slider is 100..1600 step 100
Anything this module rejects is data the app UI could never have produced.

Usage:
  python3 workout_contract.py requests.json     # file = list of [path, body] (dry-run output); exit 1 on any error
  python3 workout_contract.py --server 2026-11-01 2026-12-31   # lint workouts already on the server
  from workout_contract import component, validate, call      # in generation scripts
NOTIFICATIONS: this module never calls push endpoints, and `call` refuses any path not in ALLOWED_PATHS.
"""
import json, re, sys, uuid as _uuid, urllib.request

BASE = "https://7uyfdpbnyj.execute-api.us-east-2.amazonaws.com/"
TYPES = {"Interval", "Tempo", "Progression", "descriptionOnly"}   # descriptionOnly is legacy/API-only; see warnings
PACES = {"SB", "SB Avg", "PR", "Goal", "Base Training Pace"}
EVENTS = {400, 800, 1600, 3200, 5000}
MMSS = re.compile(r"^\d{1,2}:\d{2}$")
ADJ = re.compile(r"^-?\d{1,2}:\d{2}(\.\d)?$")

# Only these are ever called by generation scripts. Anything else (push/notification endpoints) is refused.
ALLOWED_PATHS = re.compile(
    r"^(xc/workout/(create|update|delete)|xc/training-run/(create|update|delete)|"
    r"cross-training-activity/(create|update|delete)|api/events/[0-9a-fA-F-]{36}/(visibility|groups))(\?.*)?$")


def component(description, type="Interval", target_event=5000, pace="Base Training Pace", percent=100,
              target_distance=400, count=1, duration="00:00", recovery="00:00",
              recovery_type="Standing", recovery_distance=0, sets=1, set_recovery="00:00",
              ratio=1.0, pace_adjustment="0:00.0"):
    """Build a component exactly as the app form would send it. Timed reps: pass duration='03:00' (keep a distance)."""
    if recovery_type == "Active" and not recovery_distance and recovery == "00:00":
        raise ValueError("Active recovery needs recovery_distance (100..1600) or a recovery time")
    return dict(uuid=str(_uuid.uuid4()), description=description[:200], type=type, pace=pace,
                targetDistance=target_distance, targetCount=count, duration=duration,
                targetPaceAdjustment=pace_adjustment, ratio=ratio, sets=sets, recovery=recovery,
                setRecovery=set_recovery, targetEvent=target_event, recoveryType=recovery_type,
                recoveryDistance=recovery_distance, percent=percent)


def validate_component(c, where=""):
    e, w = [], []
    add = lambda m: e.append(f"{where}{m}")
    if not c.get("uuid"): add("missing uuid (create returns 400)")
    d = c.get("description") or ""
    if not d: add("empty description (form requires it)")
    if len(d) > 200: add(f"description {len(d)} chars > 200 (500 + dropped components)")
    if c.get("type") not in TYPES: add(f"type {c.get('type')!r} not in {sorted(TYPES)}")
    if c.get("pace") not in PACES: add(f"pace {c.get('pace')!r} not in {sorted(PACES)}")
    if c.get("targetEvent") not in EVENTS: add(f"targetEvent {c.get('targetEvent')!r} not in {sorted(EVENTS)}")
    if c.get("type") == "descriptionOnly":
        # API-only legacy type: the app renders it fine with null recovery fields (it guards recoveryType != nil).
        if not c.get("targetEvent"): add("targetEvent missing")
        w.append(f"{where}descriptionOnly: no paces will be generated")
        return e, w
    if not c.get("targetDistance"): add("targetDistance is 0/null (form requires non-zero, even for timed reps)")
    for f in ("duration", "recovery", "setRecovery"):
        v = c.get(f)
        if v is None: add(f"{f} is null (form always sends mm:ss, '00:00' when unused)")
        elif not MMSS.match(v): add(f"{f}={v!r} not mm:ss")
    if not ADJ.match(c.get("targetPaceAdjustment") or ""): add(f"targetPaceAdjustment={c.get('targetPaceAdjustment')!r} unexpected")
    rt = c.get("recoveryType")
    if rt not in ("Standing", "Active"): add(f"recoveryType {rt!r} must be Standing or Active (never null)")
    rd = c.get("recoveryDistance")
    if rd is None: add("recoveryDistance is null (app force-unwraps it for Active -> CRASH)")
    elif rt == "Active" and rd == 0:
        w.append(f"{where}Active recovery with recoveryDistance 0 (the form's 'time based' toggle sends this); plan screen shows 'Recovery Jog: 0'")
    if c.get("percent") is None: add("percent is null (form always sends an Int)")
    elif not 50 <= c["percent"] <= 120: add(f"percent {c['percent']} outside form range 50-120")
    if c.get("sets") is None: add("sets is null (form always sends an Int)")
    if c.get("pace") == "Base Training Pace" and c.get("type") != "descriptionOnly" and c.get("percent") == 100 \
            and c.get("targetEvent") == 5000 and "@" in d and "100%" not in d:
        w.append(f"{where}description mentions an effort percent but percent=100; check it")
    return e, w


def validate(path, body):
    """Return (errors, warnings) for one [path, body] create/update request."""
    e, w = [], []
    if not ALLOWED_PATHS.match(path): e.append(f"{path}: not an allowed (non-notification) endpoint")
    if body.get("team") != "UA" and body.get("team") is not None: w.append(f"team={body.get('team')!r} (expected UA)")
    if body.get("season") not in ("track", "xc", None): e.append(f"season {body.get('season')!r}")
    if "workout" in path and not path.startswith("api/"):
        if not body.get("uuid"): e.append("workout uuid missing")
        if not body.get("components"): e.append("workout has no components (form requires at least one)")
        name = body.get("title", "?")
        for i, c in enumerate(body.get("components", [])):
            ce, cw = validate_component(c, f"[{name}] component {i+1}: ")
            e += ce; w += cw
    return e, w


def call(path, body=None, method="POST"):
    """The only way scripts should hit the API. Refuses anything that could send a notification."""
    if not ALLOWED_PATHS.match(path):
        raise PermissionError(f"refused: {path} is not an allowed create/update/visibility/group endpoint")
    req = urllib.request.Request(BASE + path, json.dumps(body).encode() if body is not None else None,
                                 {"Content-Type": "application/json", "Accept": "application/json"}, method=method)
    return urllib.request.urlopen(req, timeout=30).status


def _lint_server(start, end):
    ws = json.load(urllib.request.urlopen(f"{BASE}xc/workout/get?startDate={start}&endDate={end}"))
    bad = 0
    for w in ws:
        e, wr = validate("xc/workout/create", w)
        for m in e: print("ERROR", w["date"], m); bad += 1
        for m in wr: print("warn ", w["date"], m)
    print(f"{len(ws)} workouts linted, {bad} errors")
    return bad


if __name__ == "__main__":
    if len(sys.argv) >= 4 and sys.argv[1] == "--server":
        sys.exit(1 if _lint_server(sys.argv[2], sys.argv[3]) else 0)
    reqs = json.load(open(sys.argv[1]))
    errs = 0
    for item in reqs:
        path, body = (item[0], item[1]) if isinstance(item, list) else (item["path"], item["body"])
        e, w = validate(path, body)
        for m in e: print("ERROR", m); errs += 1
        for m in w: print("warn ", m)
    print(f"{len(reqs)} requests, {errs} errors")
    sys.exit(1 if errs else 0)
