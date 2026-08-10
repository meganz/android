---
name: create-crash-ticket
description: >
  Creates a Jira Bug ticket from a Firebase Crashlytics issue. Fetches crash/ANR
  details (stack trace, device info, impact) and creates a structured Jira ticket.
triggers:
  - /create-crash-ticket
  - create crash ticket
  - crashlytics to jira
---

# Create Crash Ticket

Create a Jira Bug ticket from a Firebase Crashlytics issue ID. Fetches crash/ANR details
(stack trace, device info, impact metrics) from Crashlytics and creates a structured Jira
ticket in the AND project.

## Usage

```
/create-crash-ticket <issueId>
/create-crash-ticket 66f1a2b3c4d5e6f7a8b9c0d1
```

## Arguments

| Argument    | Description                        | Example                    |
|-------------|------------------------------------|----------------------------|
| `<issueId>` | Firebase Crashlytics issue ID      | `66f1a2b3c4d5e6f7a8b9c0d1`|

## Configuration

All steps below reference these values — if any change, update them here only.

- **Firebase App ID** (`APP_ID`): `1:268821755439:android:9b611c50c9f7a503`
- **Firebase Project** (`PROJECT`): `megapoc-25443`
- **Jira Project Key**: `AND`
- **Jira Base URL** (`JIRA_BASE_URL`): Read from environment variable `JIRA_BASE_URL`
- **Jira Auth**: Bearer token from environment variable `JIRA_ACCESS_TOKEN`

## Steps

### Step 1 -- Validate input and environment

**1a. Check environment variables.** Before doing anything else, verify that the
required environment variables are set by running:

```bash
python3 -c "
import os, sys
missing = [v for v in ('JIRA_BASE_URL', 'JIRA_ACCESS_TOKEN') if not os.environ.get(v)]
if missing:
    print(f'ERROR: Missing environment variables: {", ".join(missing)}', file=sys.stderr)
    sys.exit(1)
print('Environment OK')
"
```

If any are missing, stop immediately and tell the user which variables need to be set.

**1b. Extract issue ID.** Extract the Crashlytics issue ID from the user's input.
The issue ID is typically a hex string. If the user provides a Crashlytics console
URL, extract the issue ID from the URL path segment after `/issues/`.

If no issue ID can be determined, ask the user to provide one.

### Step 2 -- Fetch issue details

Call the `mcp__plugin_firebase_firebase__crashlytics_get_issue` tool:

| Parameter | Value |
|-----------|-------|
| `appId`   | `APP_ID` |
| `issueId` | `<issueId>` |

From the response, extract:
- `errorType`: one of `"ANR"`, `"FATAL"`, or `"NON_FATAL"`
- `title`: issue title (e.g., class/method name)
- `subtitle`: issue subtitle (e.g., human-readable description)
- `variants`: the array of variants (each has `id`, `sampleEvent`, `uri`)
- `variantCount`: the length of the `variants` array
- `signals`: array of signal objects (e.g., `SIGNAL_FRESH`, `SIGNAL_REGRESSED`,
  `SIGNAL_EARLY`, `SIGNAL_REPETITIVE`), each with a `description` string
- `firstSeenVersion`: the app version where this issue was first observed
- `lastSeenVersion`: the most recent app version where this issue was observed

### Step 3 -- Fetch impact metrics

Use the `topVariants` report, which supports filtering by `issueId` directly and
works reliably regardless of issue ranking.

Make **two calls in parallel** — one for the last 7 days and one for the last 90 days:

**Call 1 — Last 7 days:**

| Parameter  | Value |
|------------|-------|
| `appId`    | `APP_ID` |
| `report`   | `topVariants` |
| `filter`   | `{"issueId": "<issueId>", "intervalStartTime": "<now minus 7 days in ISO 8601>", "intervalEndTime": "<now in ISO 8601>"}` |
| `pageSize` | `20` |

**Call 2 — Last 90 days:**

| Parameter  | Value |
|------------|-------|
| `appId`    | `APP_ID` |
| `report`   | `topVariants` |
| `filter`   | `{"issueId": "<issueId>", "intervalStartTime": "<now minus 90 days in ISO 8601>", "intervalEndTime": "<now in ISO 8601>"}` |
| `pageSize` | `20` |

**IMPORTANT**: Always specify `intervalStartTime` and `intervalEndTime` explicitly.
Without explicit times, the API defaults to calendar-day-aligned boundaries which
produces different counts than the console shows.

From **each** response, extract for each variant group:
- variant ID (from the group's variant identifier)
- `metrics[0].eventsCount`: occurrences for this variant
- `metrics[0].impactedUsersCount`: affected users for this variant

**Compute totals** for each time window by summing across all variant groups:
- `eventsCount7d` / `eventsCount90d`: sum of all variant `eventsCount` values
- `impactedUsersCount7d` / `impactedUsersCount90d`: sum of all variant `impactedUsersCount` values

Use the **7-day response** (Call 1) to determine the primary variant and per-variant
metrics for the Variants table. The first entry (highest event count) is the
**primary variant**. Record its variant ID as `primaryVariantId`.

If `variantCount > 1`, join the variant metrics with the `variants` array from Step 2
by variant ID to get each variant's `uri` (Crashlytics console link).

If either call fails, default the corresponding values to `"unknown"`.

### Step 4 -- Fetch stack trace and device info

Call the `mcp__plugin_firebase_firebase__crashlytics_list_events` tool:

| Parameter | Value |
|-----------|-------|
| `appId`   | `APP_ID` |
| `filter`  | see below |

- If `variantCount > 1` and `primaryVariantId` is known:
  `{"issueId": "<issueId>", "issueVariantId": "<primaryVariantId>"}`
- Otherwise:
  `{"issueId": "<issueId>"}`

This ensures we get the stack trace for the **highest-impact variant**, not just
the most recent event (which might be from a low-impact variant).

From the first event in the response, extract:
- `device.displayName`: device model display name (e.g., `"vivo (V2529)"`)
- `operatingSystem.displayVersion`: Android version number (e.g., `"16"`)
- `version.displayVersion`: app version (e.g., `"16.0.1(260701146)(1801451b03)"`)
- `threads`: full stack trace text (multi-line string). **Include every frame** — do
  not omit or truncate any frames. Copy the entire `threads` content exactly as returned.
- `blameFrame.symbol`: the blamed function name

If the events list is empty, note that no event data is available and proceed with
what was gathered in Steps 2 and 3.

**Note**: Steps 3 and 4 are independent — run them in parallel for faster execution.

### Step 5 -- Build Jira ticket fields

#### Title

The title MUST be specific to the root cause visible in the stack trace. Never use
generic descriptions like "slow operations in main thread". Instead, identify the
key class and method from the stack trace.

**How to find the key class/method for the title:**
1. Look at the `threads` stack trace from Step 4.
2. Find the first frame that belongs to the app's own code (packages starting with
   `mega.privacy.android`, `nz.mega.sdk`, or the app's own classes). Ignore Android
   framework frames (`android.os.*`, `java.lang.*`, `androidx.*`) and native frames
   (`syscall`, `pthread_*`, `std::*`).
3. Extract the class name and method name from that frame.
4. Use the format: `ClassName.methodName`

**Title format:**
- If `errorType` is `"ANR"`:
  - Title: `[ANR] {ClassName.methodName}`
  - Example: `[ANR] UserAttributeDatabaseUpdater.checkMyChatFilesFolderRequest`
  - If no app-level frame is found, fall back to `blameFrame.symbol`
- If `errorType` is `"FATAL"` or `"NON_FATAL"`:
  - Title: `[Crash] {ExceptionClass} in {ClassName.methodName}`
  - Example: `[Crash] NullPointerException in TransferService.onStartCommand`
  - Use the issue `title` for the exception class and the first app-level frame
    for the location. If `title` is very long (contains file paths like
    `[libmega.so]`), use the `blameFrame.symbol` or the exception class only.

#### Component

- If `errorType` is `"ANR"`:
  - Component: `{"name": "Firebase Crashlytics - ANR"}`
- If `errorType` is `"FATAL"` or `"NON_FATAL"`:
  - Component: `{"name": "Crashlytics"}`

#### Description (Jira Wiki Markup)

Build the description body using Jira wiki markup format:

**Single-variant template** (when `variantCount == 1`):

```
h3. Crashlytics Issue
[View in Crashlytics|https://console.firebase.google.com/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/<issueId>?time=last-seven-days]
* *Issue ID*: <issueId>

h3. Issue Type
<ANR / Fatal Crash / Non-Fatal Crash>

h3. Signals & Versions
* *Signals*: <comma-separated list of signal names, or "None">
* *First Seen Version*: <firstSeenVersion>
* *Last Seen Version*: <lastSeenVersion>
<for each signal, include its description on a new line:>
_(signal description, e.g., "This issue first appeared 5 days ago.")_

h3. Impact
||Period||Affected Users||Occurrences||
|Last 7 days|<impactedUsersCount7d>|<eventsCount7d>|
|Last 90 days|<impactedUsersCount90d>|<eventsCount90d>|

h3. Device Info
* *Model*: <device.displayName>
* *OS Version*: Android <operatingSystem.displayVersion>
* *App Version*: <version.displayVersion>

h3. Stack Trace
{code}
<threads content>
{code}
```

**Multi-variant template** (when `variantCount > 1`):

```
h3. Crashlytics Issue
[View in Crashlytics|https://console.firebase.google.com/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/<issueId>?time=last-seven-days]
* *Issue ID*: <issueId>

h3. Issue Type
<ANR / Fatal Crash / Non-Fatal Crash>

h3. Signals & Versions
* *Signals*: <comma-separated list of signal names, or "None">
* *First Seen Version*: <firstSeenVersion>
* *Last Seen Version*: <lastSeenVersion>
<for each signal, include its description on a new line:>
_(signal description, e.g., "This issue first appeared 5 days ago.")_

h3. Impact
||Period||Affected Users||Occurrences||
|Last 7 days|<impactedUsersCount7d>|<eventsCount7d>|
|Last 90 days|<impactedUsersCount90d>|<eventsCount90d>|
* *Variants*: <variantCount>

h3. Variants
This issue has <variantCount> variants. The primary variant (stack trace shown below) accounts for <primaryVariant.eventsCount> events across <primaryVariant.impactedUsersCount> users.

||#||Events||Users||Link||
|1 (primary)|<eventsCount>|<usersCount>|[View|<variant uri>]|
|2|<eventsCount>|<usersCount>|[View|<variant uri>]|
|...|...|...|...|

h3. Device Info
* *Model*: <device.displayName>
* *OS Version*: Android <operatingSystem.displayVersion>
* *App Version*: <version.displayVersion>

h3. Stack Trace (Primary Variant)
{code}
<threads content>
{code}
```

**Multi-variant table rules:**
- Cap the table at 10 rows. If there are more than 10 variants, add a note below
  the table: `_...and N more variants. [View all in Crashlytics|<issue uri>]_`
- Variant URIs come from the `variants` array in the Step 2 response, joined by
  variant ID with the metrics from Step 3b.
- If a variant has metrics but no matching URI, omit the Link column for that row.

Map `errorType` to the display string:
- `"ANR"` -> `ANR`
- `"FATAL"` -> `Fatal Crash`
- `"NON_FATAL"` -> `Non-Fatal Crash`

**Signals & Versions section rules:**
- List signal names as a comma-separated string (e.g., `Fresh, Regressed`). Use
  human-readable names: `SIGNAL_FRESH` → `Fresh`, `SIGNAL_REGRESSED` → `Regressed`,
  `SIGNAL_EARLY` → `Early`, `SIGNAL_REPETITIVE` → `Repetitive`.
- If the `signals` array is empty or absent, set Signals to `None` and omit the
  description lines.
- Always include `firstSeenVersion` and `lastSeenVersion` even when there are no signals.

**IMPORTANT**: Include the complete stack trace — do not omit or summarize any frames.
Copy every frame exactly as returned by the Crashlytics event.

#### Full Jira payload

```json
{
    "fields": {
        "project": {"key": "AND"},
        "summary": "<title from above>",
        "issuetype": {"name": "Bug"},
        "components": [<component from above>],
        "description": "<description from above>",
        "customfield_10500": {"value": "OPEX"},
        "customfield_10501": {"value": "Cloud/Default"}
    }
}
```

### Step 6 -- Check for existing Jira ticket

Before creating a new ticket, search Jira to see if a ticket already exists for this
Crashlytics issue.

Run a Python script using the Bash tool to search Jira via JQL:

```bash
python3 << 'PYEOF'
import urllib.request, urllib.parse, json, os, sys

token = os.environ.get('JIRA_ACCESS_TOKEN')
if not token:
    print('ERROR: JIRA_ACCESS_TOKEN environment variable is not set', file=sys.stderr)
    sys.exit(1)

jira_base_url = os.environ.get('JIRA_BASE_URL')
if not jira_base_url:
    print('ERROR: JIRA_BASE_URL environment variable is not set', file=sys.stderr)
    sys.exit(1)

issue_id = "<issueId>"
jql = f'project = "AND" AND description ~ "{issue_id}" AND status != Closed'
params = urllib.parse.urlencode({'jql': jql, 'fields': 'key,summary,status'})
url = f'{jira_base_url}/rest/api/2/search?{params}'

req = urllib.request.Request(url, headers={
    'Authorization': f'Bearer {token}',
    'Content-Type': 'application/json'
}, method='GET')

try:
    resp = urllib.request.urlopen(req)
    result = json.loads(resp.read())
    print(json.dumps(result))
except urllib.error.HTTPError as e:
    error_body = e.read().decode('utf-8', errors='replace')
    print(f'ERROR {e.code}: {error_body}', file=sys.stderr)
    sys.exit(1)
PYEOF
```

From the response, check `total`:

- **If `total > 0`** (duplicate found):
  - Extract the first issue's `key`, `summary`, and `status.name`
  - Print to the user:
    ```
    Existing Jira ticket found for this Crashlytics issue:
      <ticketKey>: <summary>
      Status: <status>
      URL: <JIRA_BASE_URL>/browse/<ticketKey>
    ```
  - **Skip Step 7** (do not create a new ticket)
  - **Proceed to Step 8** (annotate Crashlytics issue) using the existing ticket key
  - After Step 8, skip to Step 9 to display results

- **If `total == 0`**: proceed to Step 7 to create the ticket

### Step 7 -- Create Jira ticket

**CRITICAL**: Do NOT use curl for the Jira API call. The `JIRA_ACCESS_TOKEN` environment
variable contains a `+` character that gets corrupted by shell expansion in curl. Use a
Python heredoc script via the Bash tool instead.

Run a Python script using the Bash tool with a heredoc to avoid shell quoting issues
with the multi-line description body:

```bash
python3 << 'PYEOF'
import urllib.request, json, os, sys

token = os.environ.get('JIRA_ACCESS_TOKEN')
if not token:
    print('ERROR: JIRA_ACCESS_TOKEN environment variable is not set', file=sys.stderr)
    sys.exit(1)

description = """<description from Step 5>"""

payload = {
    "fields": {
        "project": {"key": "AND"},
        "summary": "<title from Step 5>",
        "issuetype": {"name": "Bug"},
        "components": [<component from Step 5>],
        "description": description,
        "customfield_10500": {"value": "OPEX"},
        "customfield_10501": {"value": "Cloud/Default"}
    }
}

jira_base_url = os.environ.get('JIRA_BASE_URL')
if not jira_base_url:
    print('ERROR: JIRA_BASE_URL environment variable is not set', file=sys.stderr)
    sys.exit(1)

url = f'{jira_base_url}/rest/api/2/issue'
data = json.dumps(payload).encode('utf-8')

req = urllib.request.Request(url, data=data, headers={
    'Authorization': f'Bearer {token}',
    'Content-Type': 'application/json'
}, method='POST')

try:
    resp = urllib.request.urlopen(req)
    result = json.loads(resp.read())
    print(json.dumps({'key': result['key'], 'id': result['id'], 'self': result['self']}))
except urllib.error.HTTPError as e:
    error_body = e.read().decode('utf-8', errors='replace')
    print(f'ERROR {e.code}: {error_body}', file=sys.stderr)
    sys.exit(1)
PYEOF
```

**Important notes for building the Python command:**
- Build the description as a Python variable (triple-quoted string), not inline in the dict.
- Escape any characters that would break the Python string literals.

If the Jira API call fails, display the error to the user and stop.

From the response, extract the `key` field (e.g., `AND-23100`). This is the Jira ticket key.

### Step 8 -- Annotate Crashlytics issue

This step is reached both after creating a new ticket (Step 7) and when an existing
ticket was found (Step 6).

First, check if the Crashlytics issue already has a note linking to this Jira ticket.
Call the `mcp__plugin_firebase_firebase__crashlytics_list_notes` tool:

| Parameter | Value |
|-----------|-------|
| `appId`   | `APP_ID` |
| `issueId` | `<issueId>` |

Check the returned notes for any that contain the ticket key (e.g., `AND-23100`).

- **If no existing note contains the ticket key**: call the
  `mcp__plugin_firebase_firebase__crashlytics_create_note` tool:

  | Parameter | Value |
  |-----------|-------|
  | `appId`   | `APP_ID` |
  | `issueId` | `<issueId>` |
  | `note`    | `Jira ticket: <JIRA_BASE_URL>/browse/<ticketKey>` |

- **If a note already contains the ticket key**: skip note creation and note
  that the annotation already exists.

### Step 9 -- Display results

Present a formatted summary report to the user using a markdown table. Use the
appropriate template below depending on the outcome.

**If an existing ticket was found (from Step 6):**

```markdown
---

## Crash Ticket Report

| Field | Details |
|-------|---------|
| **Result** | Existing ticket found |
| **Jira** | [<ticketKey>](<JIRA_BASE_URL>/browse/<ticketKey>) |
| **Status** | <status> |
| **Type** | <ANR / Fatal Crash / Non-Fatal Crash> |
| **Title** | <summary> |
| **Impact (7d)** | <impactedUsersCount7d> users / <eventsCount7d> events |
| **Impact (90d)** | <impactedUsersCount90d> users / <eventsCount90d> events |
| **Variants** | <variantCount> |
| **Crashlytics** | [View Issue](https://console.firebase.google.com/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/<issueId>?time=last-seven-days) |
| **Firebase Note** | <Added / Already exists> |

---
```

**If a new ticket was created — single-variant:**

```markdown
---

## Crash Ticket Report

| Field | Details |
|-------|---------|
| **Result** | New ticket created |
| **Jira** | [<ticketKey>](<JIRA_BASE_URL>/browse/<ticketKey>) |
| **Type** | <ANR / Fatal Crash / Non-Fatal Crash> |
| **Title** | <ticket title> |
| **Impact (7d)** | <impactedUsersCount7d> users / <eventsCount7d> events |
| **Impact (90d)** | <impactedUsersCount90d> users / <eventsCount90d> events |
| **Crashlytics** | [View Issue](https://console.firebase.google.com/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/<issueId>?time=last-seven-days) |
| **Firebase Note** | Added |

---
```

**If a new ticket was created — multi-variant:**

```markdown
---

## Crash Ticket Report

| Field | Details |
|-------|---------|
| **Result** | New ticket created |
| **Jira** | [<ticketKey>](<JIRA_BASE_URL>/browse/<ticketKey>) |
| **Type** | <ANR / Fatal Crash / Non-Fatal Crash> |
| **Title** | <ticket title> |
| **Impact (7d)** | <impactedUsersCount7d> users / <eventsCount7d> events |
| **Impact (90d)** | <impactedUsersCount90d> users / <eventsCount90d> events |
| **Variants** | <variantCount> (primary: <primaryVariant.eventsCount> events, <primaryVariant.impactedUsersCount> users) |
| **Crashlytics** | [View Issue](https://console.firebase.google.com/project/megapoc-25443/crashlytics/app/android:mega.privacy.android.app/issues/<issueId>?time=last-seven-days) |
| **Firebase Note** | Added |

---
```
