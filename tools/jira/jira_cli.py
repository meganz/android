#!/usr/bin/env python3
"""
Jira CLI helper used by the /jira, /create-branch, and /create-mr skills,
and runnable directly from a terminal.

This script encapsulates ALL deterministic logic (HTTP, token loading,
transition matching, field discovery, branch parsing, diff classification)
so the SKILL.md files only need to handle LLM-judgment work (text
generation, prompting, decisions).

Usage:
    tools/jira/jira <subcommand> [args...]                 # via wrapper
    python3 tools/jira/jira_cli.py <subcommand> [args...]  # direct

Conventions:
    - All Jira HTTP goes through `curl` (honors ALL_PROXY for SOCKS5 users
      without any per-environment config in the skill).
    - Token is read from $JIRA_TOKEN, falling back to grepping ~/.zshrc.
    - Targets MEGA's Jira Server / Data Center at
      https://jira.developers.mega.co.nz (REST /rest/api/2/, Bearer PAT).
    - All `KEY` arguments are issue keys like AND-1234 (uppercase).
    - Multi-line bodies for `comment` and `update-field` are read from
      stdin to avoid shell-quoting pitfalls.

Exit codes:
    0  ok
    1  caller-side error: not-found (HTTP 404), failed input validation,
       missing base ref, empty-stdin guard, or any other 4xx the caller can
       fix (e.g. HTTP 400 field-too-long, 409 bad transition, 422 validation)
    2  auth / token missing or rejected (HTTP 401/403)
    3  network or genuinely unexpected HTTP (curl failure, VPN down, or 5xx)
    4  no matching transition
    5  no matching custom field
"""

import argparse
import json
import os
import pathlib
import re
import subprocess
import sys
import tempfile
import urllib.parse

# Default points at MEGA's production Jira (Server / Data Center).
# Override via $JIRA_BASE_URL for staging, sandbox, or future Cloud
# migration without editing source. Use `or default` (not `get(..., default)`)
# so that an explicitly-set-but-empty JIRA_BASE_URL also falls back — CI
# shells sometimes pre-declare all env vars as empty strings.
BASE_URL = (
    os.environ.get("JIRA_BASE_URL") or "https://jira.developers.mega.co.nz"
).rstrip("/")

# Top-level dirs that contain Kotlin/Java/Compose/XML code where a change is
# usually QA-visible. Keep in sync with the actual MEGA module layout; verified
# against `ls -d */` 2026-06. Intentionally excludes test modules
# (`core-test/`, `core-ui-test/`), build/perf tooling (`baselineprofile/`,
# `lint/`, `tools/`, `jenkinsfile/`, `gradle/`, `build-logic/`),
# and the native SDK (`sdk/` — JNI bindings, separate QA flow).
PRODUCTIVE_ROOTS = (
    "app/",
    "feature/",
    "core/",
    "domain/",
    "data/",
    "shared/",          # ~300 .kt of shared production code
    "navigation/",      # Nav3 destinations and routing
    "legacy-core-ui/",  # legacy Compose components still shipped
    "resources/",       # localized strings, assets
    "third-party-lib/", # bundled forks with runtime impact
)
# Note: `presentation/` is intentionally NOT here — it is a nested package
# inside feature/ and core/ modules, not a top-level directory.

SKIP_PATTERNS = [
    r".*Test\.kt$",
    r".*Spec\.kt$",
    r".*/test/.*",
    r".*/androidTest/.*",
    r".*\.md$",
    r".*\.txt$",
    r".*CHANGELOG.*",
    r".*README.*",
    r".*\.gradle\.kts$",
    r".*\.gradle$",
    r".*settings\.gradle.*",
    r".*proguard-rules\.pro$",
    r".*\.toml$",
    r".*\.gitignore$",
    r".*\.editorconfig$",
    r"^\.claude/.*",
    r"^\.idea/.*",
    r"^\.github/.*",
    r"^\.gitlab/.*",
]
SKIP_RE = [re.compile(p) for p in SKIP_PATTERNS]

TICKET_RE = re.compile(r"AND-\d+", re.IGNORECASE)

# Strict validators used at subcommand entry to block path-traversal-style
# inputs from reaching curl. Each value is f-string-interpolated into a
# Jira REST URL path or a custom-field key, so we accept ONLY the shapes
# Jira itself uses.
ISSUE_KEY_RE = re.compile(r"^[A-Z][A-Z0-9]+-\d+$")
FIELD_ID_RE = re.compile(r"^(customfield_\d+|[a-zA-Z][a-zA-Z0-9]*)$")
TRANSITION_ID_RE = re.compile(r"^\d+$")
# Jira Server usernames: letters/digits plus . _ @ - (email-style names are
# common). Goes into a JSON body, not a URL path, but keep it strict anyway.
USERNAME_RE = re.compile(r"^[A-Za-z0-9._@-]+$")


def _validate(value, regex, name):
    """Die with exit 1 if value doesn't match the expected shape."""
    if not regex.fullmatch(value or ""):
        die(f"invalid {name}: {value!r}", 1)


def die(msg, code=1):
    print(msg, file=sys.stderr)
    sys.exit(code)


TOKEN_ENV_VARS = ("JIRA_TOKEN", "JIRA_ACCESS_TOKEN")


def _sanitize_token(raw, *, source="env"):
    """Strip whitespace; soft-reject tokens with embedded CR/LF.

    A token containing '\n' or '\r' would, if written into the auth header
    file (jira()), let curl read additional `Header: value` lines from
    -H @file and silently attach attacker-chosen headers.

    Soft-reject (return None + warn to stderr) instead of die() so the
    caller's fallback chain — JIRA_TOKEN → JIRA_ACCESS_TOKEN → ~/.zshrc
    grep — can try the next candidate. Only when all candidates are
    exhausted does load_token's caller hard-fail.
    """
    if raw is None:
        return None
    t = raw.strip()
    if not t:
        return None
    if "\n" in t or "\r" in t:
        print(
            f"warning: JIRA token from {source} contains a newline/CR — "
            f"ignoring (would let curl inject headers via -H @file).",
            file=sys.stderr,
        )
        return None
    return t


def _parse_shell_rhs(raw):
    """Extract the value from the RHS of a shell `export VAR=<raw>` line.

    Mirrors how the shell itself assigns, so a hand-edited ~/.zshrc parses the
    same way `source ~/.zshrc` would:
      - A double/single-quoted value spans to its matching closing quote;
        anything after it (e.g. a trailing ` # comment`) is ignored.
      - An unquoted value ends at the first whitespace — so a trailing
        ` # comment` or stray argument is dropped rather than captured into
        the token (which would silently corrupt the PAT).

    Returns the de-quoted value, or "" when empty.
    """
    raw = raw.strip()
    if not raw:
        return ""
    q = raw[0]
    if q in ('"', "'"):
        end = raw.find(q, 1)
        return raw[1:] if end == -1 else raw[1:end]  # unterminated → take rest
    return raw.split()[0]


def load_token():
    """Find the Jira PAT.

    Order:
      1. $JIRA_TOKEN
      2. $JIRA_ACCESS_TOKEN  (legacy name used by create-crash-ticket skill)
      3. `export JIRA_TOKEN=...` in ~/.zshrc
      4. `export JIRA_ACCESS_TOKEN=...` in ~/.zshrc

    Returns a sanitized (stripped, newline-rejected) token or None.
    """
    for v in TOKEN_ENV_VARS:
        t = _sanitize_token(os.environ.get(v), source=f"${v}")
        if t:
            return t
    zshrc = pathlib.Path.home() / ".zshrc"
    if zshrc.exists():
        pattern = re.compile(
            r"\s*export\s+(?:" + "|".join(TOKEN_ENV_VARS) + r")=(.*)$"
        )
        for line in zshrc.read_text().splitlines():
            m = pattern.match(line)
            if m:
                t = _sanitize_token(
                    _parse_shell_rhs(m.group(1)),
                    source="~/.zshrc",
                )
                if t:
                    return t
    return None


def jira(method, path, body=None):
    token = load_token()
    if not token:
        die(
            "Neither JIRA_TOKEN nor JIRA_ACCESS_TOKEN is set. "
            "Add `export JIRA_TOKEN=<pat>` to ~/.zshrc and reopen the shell.",
            2,
        )

    # The Bearer token must NOT appear in curl's argv (would be visible to
    # any local user via `ps auxww`). Write the Authorization header to a
    # 0600 temp file and pass it via `-H @path`. The file is unlinked
    # immediately after curl exits, in a finally to survive crashes.
    fd, hdr_path = tempfile.mkstemp(prefix="jira_hdr_", suffix=".txt")
    try:
        os.fchmod(fd, 0o600)
        os.write(fd, f"Authorization: Bearer {token}\n".encode())
        os.close(fd)

        # -q disables ~/.curlrc — without it, a user's `verbose` or
        # `trace-ascii` setting would print outgoing Authorization headers
        # to stderr, which we then surface via die() and the PAT lands in
        # scrollback / transcripts.
        cmd = [
            "curl", "-q", "-sS", "--max-time", "30",
            "-w", "\n__HTTP_CODE__%{http_code}",
            "-X", method,
            "-H", f"@{hdr_path}",
            "-H", "Accept: application/json",
        ]
        input_bytes = None
        if body is not None:
            cmd += ["-H", "Content-Type: application/json", "--data-binary", "@-"]
            input_bytes = body if isinstance(body, bytes) else json.dumps(body).encode()
        cmd.append(f"{BASE_URL}/rest/api/2{path}")

        r = subprocess.run(cmd, input=input_bytes, capture_output=True)
    finally:
        # Token file MUST be removed even on weird OS errors (permission
        # changes mid-run, full tmpfs, EINTR). Broaden to OSError so the file
        # never lingers with cleartext PAT.
        try:
            os.unlink(hdr_path)
        except OSError:
            pass

    if r.returncode != 0:
        # Defense-in-depth: even though -q disables ~/.curlrc, redact any
        # Authorization line before surfacing curl's stderr to the user.
        redacted = re.sub(
            r"(?i)(authorization\s*:\s*bearer\s+)\S+", r"\1<REDACTED>",
            r.stderr.decode(errors="replace"),
        )
        die(
            f"curl failed (returncode={r.returncode}): {redacted[:300]}\n"
            f"Is your VPN / SOCKS5 proxy reachable?",
            3,
        )
    out = r.stdout.decode()
    if "\n__HTTP_CODE__" not in out:
        die(f"unexpected curl output (no status code): {out[:200]}", 3)
    body_str, code_str = out.rsplit("\n__HTTP_CODE__", 1)
    return int(code_str.strip()), body_str.strip()


def expect_status(code, body, expected):
    if isinstance(expected, int):
        expected = (expected,)
    if code in expected:
        return
    if code in (401, 403):
        die(f"Auth failed (HTTP {code}). Refresh JIRA_TOKEN in ~/.zshrc.", 2)
    if code == 404:
        die(f"Not found (HTTP 404): {body[:200]}", 1)
    # 4xx (other than 401/403/404) is a caller error: malformed payload,
    # field-too-long (the documented 32767-char Test Instruction limit
    # returns 400), bad transition for current status (400/409), validation
    # failure (422). Map to rc=1 so callers don't mistake them for network
    # outages (rc=3).
    if 400 <= code < 500:
        die(f"Caller error (HTTP {code}): {body[:300]}", 1)
    # 5xx / unknown / truly unexpected → rc=3
    die(f"HTTP {code}: {body[:300]}", 3)


# ---------- subcommands ----------

# Note: there is intentionally NO `token` subcommand. The token must never
# leave this process via stdout — that would land in scrollback, transcripts,
# `set -x` logs, or shared screen captures. Token is loaded only inside
# `jira()` for the duration of one HTTP call.

def cmd_status(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    code, body = jira("GET", f"/issue/{args.key}?fields=summary,status,assignee,customfield_10400,parent")
    expect_status(code, body, 200)
    data = json.loads(body)
    f = data["fields"]
    a = f.get("assignee") or {}
    ti = f.get("customfield_10400")
    parent = f.get("parent")
    out = {
        "key": data["key"],
        "summary": f["summary"],
        "status": f["status"]["name"],
        "assignee": a.get("displayName", "unassigned"),
        "test_instruction_empty": not ti or not str(ti).strip(),
        "parent_key": parent.get("key") if parent else None,
        "parent_summary": (parent.get("fields") or {}).get("summary") if parent else None,
        "browse_url": f"{BASE_URL}/browse/{data['key']}",
    }
    print(json.dumps(out, indent=2))


def cmd_transitions(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    code, body = jira("GET", f"/issue/{args.key}/transitions")
    expect_status(code, body, 200)
    data = json.loads(body)
    out = [
        {"id": t["id"], "name": t["name"], "to": t["to"]["name"]}
        for t in data.get("transitions", [])
    ]
    print(json.dumps(out, indent=2))


def cmd_pick_transition(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    cands = [c.strip() for c in args.candidates.split(",") if c.strip()]
    code, body = jira("GET", f"/issue/{args.key}/transitions")
    expect_status(code, body, 200)
    trans = json.loads(body).get("transitions", [])

    # Match by target status name (transition.to.name), not transition.name.
    # Workflow configurations commonly name transitions with verbs like
    # "Send to Tech QA" while the target status is just "Tech QA". Matching
    # on the target status is stable across workflow naming differences.
    #
    # Two-pass: exact case-insensitive match first, then substring. Without
    # this, candidate "QA" would substring-match "Tech QA" and silently pick
    # the wrong transition.
    def emit(t):
        print(json.dumps({"id": t["id"], "name": t["name"], "to": t["to"]["name"]}))

    for c in cands:
        for t in trans:
            if c.lower() == t["to"]["name"].lower():
                emit(t)
                return
    for c in cands:
        for t in trans:
            if c.lower() in t["to"]["name"].lower():
                emit(t)
                return
    available = ", ".join(t["to"]["name"] for t in trans)
    die(f"No transition target matches {cands}. Available: {available}", 4)


def cmd_transition(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    _validate(args.transition_id, TRANSITION_ID_RE, "transition id")
    code, body = jira("POST", f"/issue/{args.key}/transitions",
                      {"transition": {"id": args.transition_id}})
    expect_status(code, body, 204)


def cmd_assign(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    _validate(args.username, USERNAME_RE, "username")
    # Jira Server / DC takes {"name": ...}; Cloud would need accountId, but
    # this CLI targets the MEGA Server instance only (see module docstring).
    code, body = jira("PUT", f"/issue/{args.key}/assignee",
                      {"name": args.username})
    expect_status(code, body, 204)


def cmd_comment(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    text = sys.stdin.read()
    if not text.strip():
        die("empty comment body on stdin", 1)
    code, body = jira("POST", f"/issue/{args.key}/comment", {"body": text})
    expect_status(code, body, 201)
    data = json.loads(body)
    url = f"{BASE_URL}/browse/{args.key}?focusedCommentId={data['id']}"
    print(json.dumps({"id": data["id"], "url": url}))


def cmd_worklog(args):
    """POST a worklog entry. Optional comment body on stdin.

    --started must be `YYYY-MM-DDTHH:MM:SS.SSS+ZZZZ` (Jira's required format,
    timezone offset with NO colon, e.g. 2026-06-25T09:00:00.000+0600).
    --time is Jira duration syntax: `5h`, `4h 30m`, `2h 20m`.
    """
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    comment = "" if sys.stdin.isatty() else sys.stdin.read()
    payload = {"timeSpent": args.time, "started": args.started}
    if comment.strip():
        payload["comment"] = comment
    if args.dry_run:
        print(json.dumps({"key": args.key, "payload": payload}, indent=2))
        return
    code, body = jira("POST", f"/issue/{args.key}/worklog", payload)
    expect_status(code, body, 201)
    data = json.loads(body)
    print(json.dumps({
        "id": data["id"],
        "timeSpent": data.get("timeSpent"),
        "started": data.get("started"),
        "browse_url": f"{BASE_URL}/browse/{args.key}",
    }, indent=2))


def cmd_worklog_delete(args):
    """Delete a worklog entry by id (e.g. to fix a mis-logged entry)."""
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    code, body = jira("DELETE", f"/issue/{args.key}/worklog/{args.id}")
    expect_status(code, body, 204)
    print(json.dumps({"deleted": args.id, "key": args.key}))


def cmd_worklogs(args):
    """List existing worklogs on an issue (for dedup before posting)."""
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    code, body = jira("GET", f"/issue/{args.key}/worklog")
    expect_status(code, body, 200)
    data = json.loads(body)
    out = [{
        "id": w["id"],
        "author": (w.get("author") or {}).get("name")
                  or (w.get("author") or {}).get("emailAddress"),
        "started": w.get("started"),
        "timeSpent": w.get("timeSpent"),
    } for w in data.get("worklogs", [])]
    print(json.dumps({"key": args.key, "total": len(out), "worklogs": out}, indent=2))


def cmd_field_id(args):
    cands = [c.strip().lower() for c in args.candidates.split(",") if c.strip()]
    code, body = jira("GET", "/field")
    expect_status(code, body, 200)
    fields = json.loads(body)

    # Iterate candidates first (outer) so caller-supplied priority is honored.
    # Two passes: exact case-insensitive match, then substring fallback.
    # Without this, a stale "Test Instructions" field would beat the intended
    # "Test Instruction" just because Jira returned it first in /field.
    match = None
    for c in cands:
        match = next((f for f in fields if f.get("name", "").lower() == c), None)
        if match:
            break
    if not match:
        for c in cands:
            match = next(
                (f for f in fields if c in f.get("name", "").lower()),
                None,
            )
            if match:
                break
    if not match:
        die(f"No field name matches {cands}", 5)
    print(json.dumps({
        "id": match["id"],
        "name": match["name"],
        "type": match.get("schema", {}).get("type"),
    }))


def cmd_read_field(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    _validate(args.field_id, FIELD_ID_RE, "field id")
    code, body = jira("GET", f"/issue/{args.key}?fields={args.field_id}")
    expect_status(code, body, 200)
    data = json.loads(body)
    v = (data.get("fields") or {}).get(args.field_id)
    if v is None:
        return  # empty
    if isinstance(v, str):
        sys.stdout.write(v)
    else:
        print(json.dumps(v))


def _lookup_field_schema(field_id):
    """Return Jira's schema dict for `field_id`, or None if absent."""
    code, body = jira("GET", "/field")
    expect_status(code, body, 200)
    for f in json.loads(body):
        if f.get("id") == field_id:
            return f.get("schema") or {}
    return None


def cmd_update_field(args):
    _validate(args.key, ISSUE_KEY_RE, "issue key")
    _validate(args.field_id, FIELD_ID_RE, "field id")
    value = sys.stdin.read()
    # Refuse to silently clear a populated field due to an unset shell var
    # or a failed upstream generation step. Clearing must be opt-in.
    if not value.strip() and not args.allow_empty:
        die(
            "empty stdin would clear the field. "
            "Pass --allow-empty to confirm an intentional clear.",
            1,
        )
    # The CLI sends `{"fields": {field_id: <raw stdin string>}}` which is the
    # correct shape ONLY for text/textarea (schema.type == "string") fields.
    # Select, multi-select, user picker, and array fields need wrapped values
    # like `{"value": "x"}` or `[{"value": "x"}]` — sending a raw string would
    # either 400 or land unexpected data. Refuse non-string types here so
    # future callers (a new skill matching a different field-id candidate
    # list) fail loudly instead of silently corrupting data.
    schema = _lookup_field_schema(args.field_id)
    if schema is not None:
        ftype = schema.get("type")
        if ftype != "string" and not args.force_type:
            die(
                f"field {args.field_id!r} has schema.type={ftype!r}, not "
                f"'string'. update-field only supports text/textarea fields. "
                f"Use --force-type if you know what you're doing.",
                1,
            )
    code, body = jira("PUT", f"/issue/{args.key}", {"fields": {args.field_id: value}})
    expect_status(code, body, 204)


def _kv(spec):
    """Split a `key=value` CLI spec. Value may itself contain '='.

    Raises ValueError on a missing '='. Returns (key.strip(), value) — the
    value is NOT stripped, so callers can pass values with leading/trailing
    spaces deliberately (rare, but Jira allows it).
    """
    if "=" not in spec:
        raise ValueError(f"expected key=value, got {spec!r}")
    k, v = spec.split("=", 1)
    return k.strip(), v


def _build_create_payload(*, project, issuetype, summary, description,
                          components=None, labels=None, priority=None,
                          selects=None, raw_fields=None):
    """Assemble the `/rest/api/2/issue` create payload from CLI inputs.

    Pure (no I/O) so it is unit-testable in `selftest`. Raises ValueError on
    any caller mistake; cmd_create maps that to exit 1.

    - `selects` are `customfield_NNNNN=VALUE` specs for single-select fields
      (wrapped as `{"value": VALUE}`) — the common MEGA case
      (customfield_10500=OPEX, customfield_10501=Cloud/Default).
    - `raw_fields` are `field=<json>` specs for anything else (cascading
      selects, arrays, user pickers); the value is parsed as JSON verbatim.
    """
    if not summary or not summary.strip():
        raise ValueError("summary is required")
    fields = {
        "project": {"key": project},
        "summary": summary,
        "issuetype": {"name": issuetype},
        "description": description,
    }
    if components:
        fields["components"] = [{"name": c} for c in components]
    if labels:
        for l in labels:
            # Jira rejects labels containing whitespace with an opaque 400;
            # fail loudly here with the offending value instead.
            if re.search(r"\s", l):
                raise ValueError(f"label {l!r} contains whitespace; Jira labels cannot")
        fields["labels"] = list(labels)
    if priority:
        fields["priority"] = {"name": priority}
    for spec in (selects or []):
        k, v = _kv(spec)
        if not FIELD_ID_RE.fullmatch(k):
            raise ValueError(f"invalid --select field id {k!r}")
        fields[k] = {"value": v}
    for spec in (raw_fields or []):
        k, v = _kv(spec)
        if not FIELD_ID_RE.fullmatch(k):
            raise ValueError(f"invalid --field field id {k!r}")
        try:
            fields[k] = json.loads(v)
        except json.JSONDecodeError as e:
            raise ValueError(f"--field {k} value is not valid JSON: {e}")
    return {"fields": fields}


def cmd_create(args):
    description = sys.stdin.read()
    # A bug with no description is almost never intended; require it unless the
    # caller explicitly opts out (mirrors update-field's --allow-empty guard).
    if not description.strip() and not args.allow_empty_description:
        die(
            "empty description on stdin. Pipe the body in, or pass "
            "--allow-empty-description to create a summary-only ticket.",
            1,
        )
    try:
        payload = _build_create_payload(
            project=args.project,
            issuetype=args.type,
            summary=args.summary,
            description=description,
            components=args.component,
            labels=args.label,
            priority=args.priority,
            selects=args.select,
            raw_fields=args.field,
        )
    except ValueError as e:
        die(str(e), 1)
    # --dry-run: print the exact payload and make NO network call. Lets the
    # caller review a batch of payloads (e.g. one per security finding) before
    # creating anything real.
    if args.dry_run:
        print(json.dumps(payload, indent=2))
        return
    code, body = jira("POST", "/issue", payload)
    expect_status(code, body, 201)
    data = json.loads(body)
    print(json.dumps({
        "key": data["key"],
        "id": data["id"],
        "browse_url": f"{BASE_URL}/browse/{data['key']}",
    }, indent=2))


def cmd_search(args):
    params = urllib.parse.urlencode({
        "jql": args.jql,
        "fields": args.fields,
        "maxResults": str(args.max),
    })
    code, body = jira("GET", f"/search?{params}")
    expect_status(code, body, 200)
    data = json.loads(body)
    issues = [{
        "key": i["key"],
        "summary": i["fields"].get("summary"),
        "status": (i["fields"].get("status") or {}).get("name"),
        "browse_url": f"{BASE_URL}/browse/{i['key']}",
    } for i in data.get("issues", [])]
    print(json.dumps({"total": data.get("total", 0), "issues": issues}, indent=2))


def cmd_components(args):
    code, body = jira("GET", f"/project/{args.project}/components")
    expect_status(code, body, 200)
    data = json.loads(body)
    print(json.dumps([{"id": c["id"], "name": c["name"]} for c in data], indent=2))


def cmd_create_meta(args):
    """Print required fields + allowed values for creating <type> in <project>.

    Lets a skill discover what the project's create screen actually requires
    (and the allowed values for select fields) instead of hardcoding
    customfield ids that drift between projects/instances.

    Uses the per-issuetype createmeta endpoints
    (`/issue/createmeta/{project}/issuetypes[/{id}]`). The legacy single-shot
    `GET /issue/createmeta?projectKeys=...&expand=...` was removed in Jira
    9.0+ (it 404s as "Issue Does Not Exist" because the router falls through
    to `/issue/{key}`), so we don't use it.
    """
    # 1. Resolve the issue-type id by name (paginated list of types).
    code, body = jira("GET", f"/issue/createmeta/{args.project}/issuetypes")
    expect_status(code, body, 200)
    types = json.loads(body).get("values", [])
    match = next((t for t in types if t.get("name", "").lower() == args.type.lower()), None)
    if not match:
        available = ", ".join(t.get("name", "") for t in types)
        die(f"No issue type {args.type!r} in {args.project}. Available: {available}", 1)

    # 2. Fetch that type's create fields (also paginated under "values").
    code, body = jira("GET", f"/issue/createmeta/{args.project}/issuetypes/{match['id']}")
    expect_status(code, body, 200)
    fields = json.loads(body).get("values", [])
    out = []
    for f in fields:
        allowed = [
            av.get("value") or av.get("name")
            for av in f.get("allowedValues", [])
        ]
        out.append({
            "id": f.get("fieldId"),
            "name": f.get("name"),
            "required": f.get("required", False),
            "allowedValues": allowed[:25] or None,
        })
    # Required fields first, then alphabetical by id — the caller mostly cares
    # about what it MUST supply.
    out.sort(key=lambda f: (not f["required"], f["id"] or ""))
    print(json.dumps(out, indent=2))


def cmd_branch_ticket(args):
    r = subprocess.run(["git", "branch", "--show-current"], capture_output=True, text=True)
    branch = r.stdout.strip()
    m = TICKET_RE.search(branch)
    if not m:
        die(f"No AND-NNNN ticket key in branch {branch!r}", 1)
    print(m.group(0).upper())


def _slugify(words):
    raw = "-".join(w.lower() for w in words)
    # Replace any run of disallowed chars with a single dash, collapse, trim.
    # Spaces, dots, underscores etc. all become "-" so multi-word args get
    # slugified correctly (e.g. "fix login crash" -> "fix-login-crash").
    raw = re.sub(r"[^a-z0-9-]+", "-", raw)
    raw = re.sub(r"-+", "-", raw)
    return raw.strip("-")


def _strip_orchestration_flags(parts):
    """Drop long-form flag tokens (those starting with '--') from branch args.

    branch-name receives a free-form description plus a ticket id. The
    create-branch orchestrator owns its own flags (--base, --no-jira,
    --no-pull) and is supposed to strip them before delegating here, but
    argparse.REMAINDER only shields *leading* flags — a flag placed after
    the ticket (e.g. `AND-1 fix login --no-jira`) would otherwise be
    slugified into the branch name. Drop any '--'-prefixed token defensively
    so a stray flag can never pollute the slug.

    Caveat: this is belt-and-suspenders, not a full substitute. The bare
    VALUE of a value-taking flag (the `main` in `--base main`) is not a
    flag token and survives — the orchestrator must still strip `--base <x>`
    as a pair. Single-dash tokens (`-leading`) are left untouched: they are
    not flags this CLI owns and are slugified harmlessly.
    """
    return [a for a in parts if not a.startswith("--")]


def cmd_branch_name(args):
    parts = args.args
    # If the first arg already contains '/', use as-is
    if parts and "/" in parts[0]:
        print(parts[0])
        return
    email = subprocess.run(["git", "config", "user.email"], capture_output=True, text=True).stdout.strip()
    if "@" not in email:
        die("git config user.email is not set", 1)
    prefix = email.split("@", 1)[0]
    # Defensively drop long-form flags that leaked past the orchestrator so
    # they can't end up in the slug (see _strip_orchestration_flags).
    words = _strip_orchestration_flags(parts)
    ticket_match = None
    for a in words:
        ticket_match = TICKET_RE.search(a)
        if ticket_match:
            break
    if not ticket_match:
        die("No AND-NNNN ticket in args", 1)
    ticket = ticket_match.group(0).upper()
    # Strip the AND-NNNN token from every arg (handles both bare `AND-1234`
    # and combined `AND-1234-fix-login`) so the ticket can't appear twice in
    # the slug. Drop args that become empty / pure separators after stripping.
    desc_words = []
    for a in words:
        cleaned = TICKET_RE.sub("", a)
        if cleaned.strip("-_ ."):
            desc_words.append(cleaned)
    slug = _slugify(desc_words)
    print(f"{prefix}/{ticket}" + (f"-{slug}" if slug else ""))


def _classify_path(p):
    if not p.startswith(PRODUCTIVE_ROOTS):
        return "skip"
    if any(rx.match(p) for rx in SKIP_RE):
        return "skip"
    return "needs"


def _git_lines(cmd, *, allow_failure=False):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        if allow_failure:
            return []
        die(
            f"git command failed: {' '.join(cmd)}\n"
            f"  stderr: {r.stderr.strip()[:300]}",
            1,
        )
    # Strip a leading './' so _classify_path's startswith() check against
    # PRODUCTIVE_ROOTS works regardless of CWD. Do NOT run os.path.normpath
    # here: git always emits forward-slash, repo-relative paths (even on
    # Windows), whereas normpath rewrites them to the OS separator ('\' on
    # Windows). That would break the forward-slash PRODUCTIVE_ROOTS match
    # (classifying all productive code as "skip" on Windows) and diverge
    # from the path form git itself uses.
    out = []
    for l in r.stdout.splitlines():
        if not l:
            continue
        if l.startswith("./"):
            l = l[2:]
        out.append(l)
    return out


def cmd_selftest(args):
    """Run stdlib unittest assertions on the pure-Python helpers.

    Covers PRODUCTIVE_ROOTS / SKIP_RE classification, _slugify, the
    validation regexes, and _sanitize_token. No network, no Jira calls
    — safe to run in any environment (CI, pre-push, fresh clone).
    """
    import unittest

    class HelperTests(unittest.TestCase):

        # --- _classify_path --------------------------------------------
        def test_productive_kotlin_classified_needs(self):
            for p in [
                "app/src/main/kotlin/x/HomeActivity.kt",
                "feature/sync/src/main/kotlin/SyncWorker.kt",
                "data/src/main/java/x/AccountRepository.kt",
                "core/ui-components/src/main/kotlin/Button.kt",
                "shared/lib/foo/SomeUseCaseImpl.kt",
                "navigation/Dest.kt",
                "legacy-core-ui/Widget.kt",
                "resources/strings.xml",
            ]:
                self.assertEqual(_classify_path(p), "needs", p)

        def test_test_files_classified_skip(self):
            for p in [
                "feature/sync/src/test/kotlin/SyncWorkerTest.kt",
                "data/src/androidTest/java/SomeTest.kt",
                "app/src/main/kotlin/FooSpec.kt",
            ]:
                self.assertEqual(_classify_path(p), "skip", p)

        def test_build_and_meta_classified_skip(self):
            for p in [
                "app/proguard-rules.pro",
                "app/build.gradle.kts",
                "gradle/catalogs/lib.versions.toml",
                ".claude/skills/jira/SKILL.md",
                "README.md",
                "build.gradle.kts",
                ".gitignore",
                "tools/jira/jira_cli.py",
            ]:
                self.assertEqual(_classify_path(p), "skip", p)

        def test_classify_requires_forward_slashes(self):
            # Guards the _git_lines fix: git emits forward-slash paths and we
            # must NOT normpath them to the OS separator. A backslashed path
            # (what normpath would produce on Windows) must be recognised as
            # the bug it represents — it would wrongly classify as "skip".
            self.assertEqual(_classify_path("app\\src\\Main.kt"), "skip")
            self.assertEqual(_classify_path("app/src/Main.kt"), "needs")

        def test_non_productive_roots_skipped(self):
            for p in [
                "baselineprofile/Foo.kt",
                "lint/Bar.kt",
                "buildSrc/Baz.kt",
                "build-logic/Qux.kt",
            ]:
                self.assertEqual(_classify_path(p), "skip", p)

        # --- _slugify --------------------------------------------------
        def test_slugify_basic(self):
            self.assertEqual(_slugify(["fix", "login"]), "fix-login")

        def test_slugify_spaces_in_arg(self):
            self.assertEqual(_slugify(["with spaces"]), "with-spaces")

        def test_slugify_strips_unicode_and_punct(self):
            self.assertEqual(_slugify(["中文", "desc"]), "desc")
            self.assertEqual(_slugify(["one_two", "three.four"]), "one-two-three-four")

        def test_slugify_collapses_and_trims(self):
            self.assertEqual(_slugify(["--", "--"]), "")
            self.assertEqual(_slugify(["a", "", "b"]), "a-b")

        # --- _strip_orchestration_flags --------------------------------
        def test_strip_flags_removes_trailing_long_flags(self):
            # The leak case: a flag after the ticket must not reach the slug.
            self.assertEqual(
                _strip_orchestration_flags(["AND-1", "fix", "login", "--no-jira"]),
                ["AND-1", "fix", "login"],
            )
            self.assertEqual(
                _strip_orchestration_flags(["AND-1", "--no-pull", "fix"]),
                ["AND-1", "fix"],
            )

        def test_strip_flags_keeps_words_and_single_dash(self):
            # Single-dash tokens are not flags we own — slugified harmlessly.
            self.assertEqual(
                _strip_orchestration_flags(["AND-1", "-leading", "desc"]),
                ["AND-1", "-leading", "desc"],
            )

        def test_strip_flags_value_flag_bare_value_survives(self):
            # Documented limitation: the bare value of `--base main` is not a
            # flag token and survives — the orchestrator must strip the pair.
            self.assertEqual(
                _strip_orchestration_flags(["AND-1", "--base", "main", "fix"]),
                ["AND-1", "main", "fix"],
            )

        # --- validators ------------------------------------------------
        def test_issue_key_accepts(self):
            for k in ["AND-1", "AND-1234", "ABC123-9999"]:
                self.assertIsNotNone(ISSUE_KEY_RE.fullmatch(k), k)

        def test_issue_key_rejects(self):
            for k in ["and-1234", "AND-1234/foo", "AND-", "AND", "../AND-1"]:
                self.assertIsNone(ISSUE_KEY_RE.fullmatch(k), k)

        def test_field_id_accepts(self):
            for f in ["customfield_10400", "summary", "assignee", "fixVersions"]:
                self.assertIsNotNone(FIELD_ID_RE.fullmatch(f), f)

        def test_field_id_rejects(self):
            for f in ["customfield_10400/foo", "summary;drop", "../foo", ""]:
                self.assertIsNone(FIELD_ID_RE.fullmatch(f), f)

        def test_transition_id_accepts(self):
            for t in ["1", "801", "99999"]:
                self.assertIsNotNone(TRANSITION_ID_RE.fullmatch(t), t)

        def test_transition_id_rejects(self):
            for t in ["foo", "1a", "../1", ""]:
                self.assertIsNone(TRANSITION_ID_RE.fullmatch(t), t)

        def test_username_accepts(self):
            for u in ["juh", "juh@mega.co.nz", "first.last", "a-b_c1"]:
                self.assertIsNotNone(USERNAME_RE.fullmatch(u), u)

        def test_username_rejects(self):
            for u in ["", "a b", "a/b", "a\nb", 'a"b']:
                self.assertIsNone(USERNAME_RE.fullmatch(u), u)

        # --- _sanitize_token ------------------------------------------
        def test_sanitize_strips_whitespace(self):
            self.assertEqual(_sanitize_token("  abc  "), "abc")

        def test_sanitize_returns_none_for_empty(self):
            self.assertIsNone(_sanitize_token(None))
            self.assertIsNone(_sanitize_token(""))
            self.assertIsNone(_sanitize_token("   "))

        def test_sanitize_rejects_newline(self):
            # Soft-reject: returns None so caller's fallback chain continues.
            self.assertIsNone(_sanitize_token("abc\ndef"))
            self.assertIsNone(_sanitize_token("abc\rdef"))

        def test_sanitize_accepts_plus_slash(self):
            # PAT format: contains / and + freely.
            self.assertEqual(_sanitize_token("MDYzMDc0Or4z+/DeqHAm"),
                             "MDYzMDc0Or4z+/DeqHAm")

        # --- _kv -------------------------------------------------------
        def test_kv_splits_on_first_equals(self):
            self.assertEqual(_kv("customfield_10500=OPEX"),
                             ("customfield_10500", "OPEX"))
            # value may contain '=' (e.g. base64 / equations)
            self.assertEqual(_kv("k=a=b"), ("k", "a=b"))
            self.assertEqual(_kv("  k =v"), ("k", "v"))

        def test_kv_rejects_missing_equals(self):
            with self.assertRaises(ValueError):
                _kv("noequals")

        # --- _build_create_payload ------------------------------------
        def test_build_payload_minimal(self):
            p = _build_create_payload(
                project="AND", issuetype="Bug",
                summary="Title", description="Body",
            )
            self.assertEqual(p, {"fields": {
                "project": {"key": "AND"},
                "summary": "Title",
                "issuetype": {"name": "Bug"},
                "description": "Body",
            }})

        def test_build_payload_full(self):
            p = _build_create_payload(
                project="AND", issuetype="Bug", summary="T", description="D",
                components=["Login", "Security"], labels=["security", "crypto"],
                priority="High",
                selects=["customfield_10500=OPEX", "customfield_10501=Cloud/Default"],
                raw_fields=['customfield_10999=["a","b"]'],
            )
            f = p["fields"]
            self.assertEqual(f["components"], [{"name": "Login"}, {"name": "Security"}])
            self.assertEqual(f["labels"], ["security", "crypto"])
            self.assertEqual(f["priority"], {"name": "High"})
            self.assertEqual(f["customfield_10500"], {"value": "OPEX"})
            self.assertEqual(f["customfield_10501"], {"value": "Cloud/Default"})
            self.assertEqual(f["customfield_10999"], ["a", "b"])

        def test_build_payload_requires_summary(self):
            for bad in (None, "", "   "):
                with self.assertRaises(ValueError):
                    _build_create_payload(
                        project="AND", issuetype="Bug",
                        summary=bad, description="D",
                    )

        def test_build_payload_rejects_label_whitespace(self):
            with self.assertRaises(ValueError):
                _build_create_payload(
                    project="AND", issuetype="Bug", summary="T", description="D",
                    labels=["has space"],
                )

        def test_build_payload_rejects_bad_field_id(self):
            with self.assertRaises(ValueError):
                _build_create_payload(
                    project="AND", issuetype="Bug", summary="T", description="D",
                    selects=["../etc=OPEX"],
                )

        def test_build_payload_rejects_bad_json_field(self):
            with self.assertRaises(ValueError):
                _build_create_payload(
                    project="AND", issuetype="Bug", summary="T", description="D",
                    raw_fields=["customfield_1=not json"],
                )

        # --- _parse_shell_rhs -----------------------------------------
        def test_parse_rhs_bare(self):
            self.assertEqual(_parse_shell_rhs("abc"), "abc")
            self.assertEqual(_parse_shell_rhs("ab/c+d=="), "ab/c+d==")

        def test_parse_rhs_quoted(self):
            self.assertEqual(_parse_shell_rhs('"abc"'), "abc")
            self.assertEqual(_parse_shell_rhs("'abc'"), "abc")
            # '#' inside quotes is part of the value, not a comment.
            self.assertEqual(_parse_shell_rhs('"ab#c"'), "ab#c")

        def test_parse_rhs_drops_trailing_comment(self):
            # The bug this fixes: an unquoted trailing comment must not be
            # captured into the token.
            self.assertEqual(_parse_shell_rhs("abc # my token"), "abc")
            self.assertEqual(_parse_shell_rhs("abc\t# c"), "abc")

        def test_parse_rhs_edge(self):
            self.assertEqual(_parse_shell_rhs(""), "")
            self.assertEqual(_parse_shell_rhs("   "), "")
            self.assertEqual(_parse_shell_rhs('"unterminated'), "unterminated")

    loader = unittest.TestLoader()
    suite = loader.loadTestsFromTestCase(HelperTests)
    runner = unittest.TextTestRunner(verbosity=2 if args.verbose else 1)
    result = runner.run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)


def cmd_classify_diff(args):
    # Pre-check that the base ref is reachable. If origin/develop (or whatever
    # --base resolves to) hasn't been fetched, every downstream `git diff
    # base...` would silently return empty — mask a real diff as "nothing
    # productive". Surface a clear remediation instead.
    rev = subprocess.run(
        ["git", "rev-parse", "--verify", "--quiet", args.base],
        capture_output=True, text=True,
    )
    if rev.returncode != 0:
        # If user passed an origin/ ref (default), suggest `git fetch origin`.
        # Otherwise suggest fetching that specific ref into a local branch.
        if args.base.startswith("origin/"):
            remedy = "Run `git fetch origin` and retry."
        else:
            remedy = f"Run `git fetch origin {args.base}:{args.base}` and retry."
        die(f"base ref {args.base!r} not found in this repo. {remedy}", 1)

    files = set()
    # The base...HEAD diff must succeed; uncommitted/staged/untracked are
    # best-effort (they can be empty without it being an error).
    files.update(_git_lines(
        ["git", "diff", "--name-only", f"{args.base}...HEAD"],
    ))
    files.update(_git_lines(["git", "diff", "--name-only"], allow_failure=True))
    files.update(_git_lines(["git", "diff", "--name-only", "--cached"], allow_failure=True))
    files.update(_git_lines(
        ["git", "ls-files", "--others", "--exclude-standard"], allow_failure=True,
    ))
    needs, skip = [], []
    for f in sorted(files):
        (needs if _classify_path(f) == "needs" else skip).append(f)
    print(json.dumps({"needs": needs, "skip": skip, "total": len(files)}, indent=2))


def main():
    p = argparse.ArgumentParser(prog="jira", description=__doc__)
    sub = p.add_subparsers(dest="cmd", required=True)

    sp = sub.add_parser("status", help="Print issue summary + status as JSON")
    sp.add_argument("key")
    sp.set_defaults(func=cmd_status)

    sp = sub.add_parser("transitions", help="List available transitions as JSON")
    sp.add_argument("key")
    sp.set_defaults(func=cmd_transitions)

    sp = sub.add_parser("pick-transition",
                        help="Find transition by target status (comma-separated candidates)")
    sp.add_argument("key")
    sp.add_argument("candidates", help='e.g. "In Progress" or "Tech QA,Code Review"')
    sp.set_defaults(func=cmd_pick_transition)

    sp = sub.add_parser("transition", help="Execute a transition by id")
    sp.add_argument("key")
    sp.add_argument("transition_id")
    sp.set_defaults(func=cmd_transition)

    sp = sub.add_parser("assign", help="Assign an issue to a username")
    sp.add_argument("key")
    sp.add_argument("username")
    sp.set_defaults(func=cmd_assign)

    sp = sub.add_parser("comment", help="POST a comment (body on stdin)")
    sp.add_argument("key")
    sp.set_defaults(func=cmd_comment)

    sp = sub.add_parser("worklog", help="POST a worklog (optional comment on stdin)")
    sp.add_argument("key")
    sp.add_argument("--time", required=True, help="Jira duration, e.g. '5h' or '4h 30m'")
    sp.add_argument("--started", required=True,
                    help="Start time: YYYY-MM-DDTHH:MM:SS.000+0600 (offset, no colon)")
    sp.add_argument("--dry-run", action="store_true",
                    help="Print the payload and make NO network call")
    sp.set_defaults(func=cmd_worklog)

    sp = sub.add_parser("worklogs", help="List existing worklogs on an issue (JSON)")
    sp.add_argument("key")
    sp.set_defaults(func=cmd_worklogs)

    sp = sub.add_parser("worklog-delete", help="Delete a worklog by id")
    sp.add_argument("key")
    sp.add_argument("id")
    sp.set_defaults(func=cmd_worklog_delete)

    sp = sub.add_parser("field-id", help="Discover custom field id by name candidates")
    sp.add_argument("candidates",
                    help='e.g. "Test Instructions,Test Instruction,QA Steps"')
    sp.set_defaults(func=cmd_field_id)

    sp = sub.add_parser("read-field", help="Read a custom field value to stdout")
    sp.add_argument("key")
    sp.add_argument("field_id")
    sp.set_defaults(func=cmd_read_field)

    sp = sub.add_parser("update-field", help="PUT a custom field (value on stdin)")
    sp.add_argument("key")
    sp.add_argument("field_id")
    sp.add_argument("--allow-empty", action="store_true",
                    help="Allow clearing the field with empty stdin")
    sp.add_argument("--force-type", action="store_true",
                    help="Allow writing a raw string to a non-string field (advanced)")
    sp.set_defaults(func=cmd_update_field)

    sp = sub.add_parser("create", help="Create an issue (description on stdin)")
    sp.add_argument("--summary", required=True, help="Issue summary / title")
    sp.add_argument("--type", default="Bug", help="Issue type name (default: Bug)")
    sp.add_argument("--project", default="AND", help="Project key (default: AND)")
    sp.add_argument("--component", action="append",
                    help="Component name (repeatable)")
    sp.add_argument("--label", action="append",
                    help="Label, no whitespace (repeatable)")
    sp.add_argument("--priority", help="Priority name, e.g. High / Medium / Low")
    sp.add_argument("--select", action="append", metavar="customfield_N=VALUE",
                    help="Single-select custom field, wrapped as {\"value\": VALUE} "
                         "(repeatable). e.g. customfield_10500=OPEX")
    sp.add_argument("--field", action="append", metavar="customfield_N=JSON",
                    help="Raw custom field; value parsed as JSON (repeatable, advanced)")
    sp.add_argument("--allow-empty-description", action="store_true",
                    help="Permit a summary-only ticket (empty stdin)")
    sp.add_argument("--dry-run", action="store_true",
                    help="Print the create payload and make NO network call")
    sp.set_defaults(func=cmd_create)

    sp = sub.add_parser("search", help="JQL search (dedup before create)")
    sp.add_argument("jql", help='JQL, e.g. \'project = AND AND summary ~ "ANDROID_ID"\'')
    sp.add_argument("--fields", default="key,summary,status",
                    help="Comma-separated fields (default: key,summary,status)")
    sp.add_argument("--max", type=int, default=50, help="Max results (default: 50)")
    sp.set_defaults(func=cmd_search)

    sp = sub.add_parser("components", help="List a project's components")
    sp.add_argument("--project", default="AND", help="Project key (default: AND)")
    sp.set_defaults(func=cmd_components)

    sp = sub.add_parser("create-meta",
                        help="Show required fields + allowed values for create")
    sp.add_argument("--project", default="AND", help="Project key (default: AND)")
    sp.add_argument("--type", default="Bug", help="Issue type name (default: Bug)")
    sp.set_defaults(func=cmd_create_meta)

    sub.add_parser("branch-ticket",
                   help="Extract AND-NNNN from current git branch").set_defaults(func=cmd_branch_ticket)

    sp = sub.add_parser("branch-name", help="Build <user>/<TICKET>-<slug>")
    # REMAINDER lets us accept positional args that start with '-' (e.g. an
    # ill-formed description like '-leading'); they get slugified anyway.
    # Note: REMAINDER only shields *leading* flags — a long-form flag placed
    # after the ticket survives into `args`, so cmd_branch_name strips '--'
    # tokens defensively (see _strip_orchestration_flags).
    sp.add_argument("args", nargs=argparse.REMAINDER)
    sp.set_defaults(func=cmd_branch_name)

    sp = sub.add_parser("classify-diff",
                        help="Classify changed files as needs|skip Test Instructions")
    # Default to the local `develop` branch — consistent with how create-mr
    # compares the working branch. If your local develop is stale relative to
    # the remote, pass `--base origin/develop` (after `git fetch origin`) to
    # diff against what a reviewer would see on the MR instead.
    sp.add_argument("--base", default="develop")
    sp.set_defaults(func=cmd_classify_diff)

    sp = sub.add_parser("selftest",
                        help="Run stdlib unittest assertions on pure-Python helpers (no network)")
    sp.add_argument("-v", "--verbose", action="store_true")
    sp.set_defaults(func=cmd_selftest)

    a = p.parse_args()
    a.func(a)


if __name__ == "__main__":
    main()
