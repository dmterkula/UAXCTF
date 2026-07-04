---
name: PRs are season-agnostic
description: PRs should not distinguish between outdoor and indoor — a PR is a PR regardless of season type
type: feedback
---

PRs should be treated the same regardless of outdoor/indoor/cc season type. A PR is a PR.

**Why:** The user views personal records as absolute bests, not segmented by season.

**How to apply:** When loading, comparing, or saving PRs in the track scripts, merge all season types and don't differentiate. The athlete_prs.csv has a season_type column but for PR detection/update purposes, treat them as one pool.