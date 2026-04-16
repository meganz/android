---
name: weblate
description: >
  Upload new Android strings to Weblate. Extracts new strings added in the current branch
  (compared to develop) from strings_shared.xml, writes them to the transifex/weblate/strings.xml
  file, runs the upload script, then optionally uploads a screenshot and maps it to the
  uploaded strings via the Weblate API.
triggers:
  - /weblate
  - upload strings
  - upload to weblate
  - sync strings to weblate
  - push strings
---

# Weblate String Upload

Upload new Android string resources to Weblate for translation. Extracts strings added in the
current branch (vs develop), writes them to the Weblate repo's `strings.xml`, runs the
upload script, and optionally uploads a screenshot mapped to the new strings.

## Usage

```
/weblate    # Upload new strings from current branch
```

## Configuration

The Weblate API config is in `transifex/weblate/translate.json`:

```json
{
    "PROJECT": "android",
    "SOURCE_TOKEN": "<token>",
    "BASE_URL": "https://translate.developers.mega.co.nz/api",
    "COMPONENT": "prod"
}
```

## Steps

### Step 1 — Pull latest in the Weblate repo

The `transifex/weblate/` directory is a separate git repository. Pull latest:

```bash
cd transifex/weblate && git pull
```

### Step 2 — Extract new strings from the current branch

Run a diff against develop to find newly added string lines (including their comment descriptions)
in the shared strings file:

```bash
git diff develop -- resources/string-resources/src/main/res/values/strings_shared.xml
```

Parse the diff output to extract all added lines (lines starting with `+` that are not `+++`).
These will be `<!-- comment -->` and `<string name="...">...</string>` lines.

If no new strings are found, inform the user and stop — there is nothing to upload.

### Step 3 — Write strings.xml

Clear all existing content in `transifex/weblate/strings.xml` and write the new strings
wrapped in the standard XML structure:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Comment description for the string -->
    <string name="string_name">String value</string>
</resources>
```

Preserve the exact comment and string pairs as they appear in the diff. Maintain 4-space
indentation inside `<resources>`.

### Step 4 — Review and upload to Weblate

**Before uploading, ask the user to review `transifex/weblate/strings.xml`.** List the strings
that will be uploaded and wait for explicit confirmation before proceeding. The upload to
Weblate is not easily reversible.

Once the user confirms, run the upload script from the project root:

```bash
./transifex/weblate/lang.sh -a android -u -f strings.xml -c strings_shared
```

This will:
1. Upload the new strings to the Weblate branch component
2. Automatically download/export strings back (modifying all locale files)

### Step 4b — Revert translated locale files

The download step modifies `strings_shared.xml` in all locale directories
(`values-ar/`, `values-de/`, etc.). Only keep the default English file (`values/strings_shared.xml`)
and revert all translated locale files — translations will be pulled separately later.

```bash
git checkout -- resources/string-resources/src/main/res/values-*/strings_shared.xml
```

Verify only `values/strings_shared.xml` remains changed:
```bash
git diff --name-only -- resources/string-resources/
```

### Step 5 — Upload screenshot and map strings

After the upload completes, ask the user:
1. **Do you have a screenshot to upload?** If yes, ask for the image path.
2. **Auto-detect strings in the screenshot:** Read the screenshot image using the Read tool
   and visually compare the text in the image against the list of uploaded strings. Propose
   which strings are visible in the screenshot.
3. **Ask the user to confirm** the proposed mapping before proceeding. The user may add or
   remove strings from the list.

If the user provides a screenshot and confirms the strings, proceed with the sub-steps below.

Read the API config from `transifex/weblate/translate.json` to get `BASE_URL` and `SOURCE_TOKEN`.

#### Step 5a — Get the branch component slug

The branch component slug follows the pattern: `strings_shared-<sanitized_branch>`

The branch name is sanitized by stripping all non-alphanumeric characters and lowercasing,
matching the logic in `transifex/weblate/python/android.py`:
```python
re.sub("[^A-Za-z0-9]+", "", branch_name).lower()
```

Get the current branch name and derive the component slug:
```bash
BRANCH=$(git branch --show-current)
SANITIZED=$(echo "$BRANCH" | sed 's/[^A-Za-z0-9]//g' | tr '[:upper:]' '[:lower:]')
COMPONENT_SLUG="strings_shared-${SANITIZED}"
```

Example: `lh/AND-23288-move-ads-free-intro-to-shared-ads` → `strings_shared-lhand23288moveadsfreeintrotosharedads`

#### Step 5b — Resize and rename the screenshot

Weblate rejects images that are too large. Scale down to max 1200px.

Also rename the screenshot to match the branch context for easy identification in Weblate.
Extract the Jira ticket ID from the branch name (e.g., `AND-23288` from
`lh/AND-23288-move-ads-free-intro-to-shared-ads`) and use it as the screenshot name:

```bash
TICKET=$(echo "$BRANCH" | grep -oE 'AND-[0-9]+')
SCREENSHOT_NAME="${TICKET:-$(echo "$BRANCH" | sed 's|.*/||')}.png"
sips -Z 1200 "<screenshot_path>" --out /tmp/weblate_screenshot.png
```

Example: branch `lh/AND-23288-move-ads-free-intro-to-shared-ads` → screenshot name `AND-23288.png`

If the user provides multiple screenshots, append a counter: `AND-23288_1.png`, `AND-23288_2.png`.

#### Step 5c — Upload the screenshot

Save the response to a temp file and parse the screenshot ID directly from it. This avoids
needing to paginate through 1000+ screenshots to find the newly created one.

```bash
curl -s -X POST \
    -H "Authorization: Token <SOURCE_TOKEN>" \
    -F "image=@/tmp/weblate_screenshot.png" \
    -F "name=<SCREENSHOT_NAME>" \
    -F "project_slug=android" \
    -F "component_slug=<component_slug>" \
    -F "language_code=en" \
    "<BASE_URL>/screenshots/" > /tmp/weblate_screenshot_response.json
```

Then extract the screenshot `id` directly from the creation response:

```bash
python3 -c "
import json
with open('/tmp/weblate_screenshot_response.json') as f:
    data = json.load(f)
print(data['id'])
"
```

If the upload fails (no `id` in response, or HTTP error), report the error and stop.

#### Step 5d — Find the unit IDs for the uploaded strings

For each string name extracted in Step 2, look up its unit ID using the `context:=` query:

```bash
curl -s -H "Authorization: Token <SOURCE_TOKEN>" \
    "<BASE_URL>/translations/android/<component_slug>/en/units/?q=context:=<string_name>&format=json" \
    > /tmp/weblate_unit.json
```

Then parse:

```bash
python3 -c "
import json
with open('/tmp/weblate_unit.json') as f:
    data = json.load(f)
for u in data['results']:
    print(u['id'])
"
```

#### Step 5e — Map strings to the screenshot

For each unit ID found, associate it with the screenshot:

```bash
curl -s -X POST \
    -H "Authorization: Token <SOURCE_TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{"unit_id": <unit_id>}' \
    "<BASE_URL>/screenshots/<screenshot_id>/units/"
```

### Step 6 — Confirm

Report the result to the user:
- List the strings that were uploaded
- Show the upload script output (success/failure)
- If screenshot was uploaded: show the screenshot name and how many strings were mapped to it
