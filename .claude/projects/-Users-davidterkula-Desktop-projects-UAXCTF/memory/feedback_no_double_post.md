---
name: Don't double-post meet results
description: When re-running scrape_track_summary.py with --redo to fix HTML, drop the --post flag to avoid posting duplicate results
type: feedback
---

When re-running the track summary script with --redo to regenerate the HTML email (e.g., to capture missed PR/SB tags), drop the --post flag to avoid posting duplicate results to the backend.

**Why:** The backend may not be fully idempotent for result creation, and posting twice could cause issues.

**How to apply:** If the user asks to redo/regenerate the email HTML after already posting, use --redo without --post.