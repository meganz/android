---
name: weblate
description: >
  Upload new Android strings to Weblate. Extracts new strings added in the current branch
  (compared to develop) from strings_shared.xml, ensures every string has a description,
  writes them to the transifex/weblate/strings.xml file, runs the upload script, sets each
  unit's translator-facing explanation, then sources a screenshot for every string — reusing
  an existing Compose screenshot-test golden, or adding a screenshot test and recording one
  when none exists (rendering fleeting components like snackbars in isolation) — and maps it
  to the uploaded strings via the Weblate API. A screenshot per string is mandatory.
triggers:
  - /weblate
  - upload strings
  - upload to weblate
  - sync strings to weblate
  - push strings
---

# Weblate String Upload

Upload new Android string resources to Weblate for translation. Extracts strings added in the
current branch (vs develop), ensures each has a description, writes them to the Weblate repo's
`strings.xml`, runs the upload script, sets each unit's `explanation`, and attaches a screenshot
for each string — preferring an existing Compose screenshot-test golden, otherwise adding a
screenshot test and recording one.

> **Enforcement:** `tools/weblate/weblate_gate.sh` verifies that every added/changed string in
> `strings_shared.xml` exists in the branch Weblate component **with a description and a mapped
> screenshot**. It runs as a Claude Code push hook (`.claude/hooks/pre-push-weblate-gate.sh`,
> denies the `git push`) and as the Jenkins MR stage "Weblate Strings Check" (fails the
> pipeline). `develop`, `master`, `release/*`, and `task/pre-release/*` branches are exempt.
> Completing this skill end-to-end satisfies both gates.

## Usage

```
/weblate    # Upload new strings from current branch
```

## Configuration

**First-time setup** (the `transifex/` directory is a separate, gitignored repo — see
`tools/weblate/README.md` for the full guide):

```bash
git clone git@code.developers.mega.co.nz:mobile/android/transifex.git transifex
cp transifex/weblate/translate.json.example transifex/weblate/translate.json
# set SOURCE_TOKEN in translate.json: Weblate → Your profile → API access
```

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

### Step 0 — Derive the feature branch (worktree-aware)

The Weblate upload script derives the branch component slug from git, but it does so by
`chdir`-ing to the **main** repo root (`transifex/weblate/python/android.py` `get_branch_name()`).
When the feature branch is checked out in a git **worktree**, the main checkout is on a
*different* branch, so the wrong slug is derived (or a `master`/`main`/`develop` branch is
rejected by `python/lang.py` `upload()`).

To make this worktree-safe, derive the feature branch from the **current working directory**
once, up front, and reuse it everywhere below (slug derivation in Step 5a, screenshot naming
in Step 5b, and the upload in Step 4):

```bash
# Run from the worktree/checkout that has the feature branch checked out (the current dir).
FEATURE_BRANCH=$(git rev-parse --abbrev-ref HEAD)
```

This value is passed explicitly to the upload script via the `gitlabBranch` environment
variable in Step 4 (`gitlabBranch=$FEATURE_BRANCH ./transifex/weblate/lang.sh ...`).

> **Prerequisite (separate repo — `transifex/weblate`):** `get_branch_name()` currently only
> honours `gitlabBranch` as a *fallback* when the `git symbolic-ref` call fails. For the
> explicit value to take effect from a worktree, `transifex/weblate/python/android.py` must
> **prefer** `gitlabBranch` when it is set. Required one-line change at
> `transifex/weblate/python/android.py:393` (`get_branch_name()`):
>
> ```python
> def get_branch_name():
>     branch_name = os.getenv("gitlabBranch", "")   # <-- prefer explicit env var first
>     if not branch_name:
>         cur_path = os.getcwd()
>         try:
>             os.chdir(script_dir + "../../../")
>             branch_name = subprocess.check_output(['git', 'symbolic-ref', '--short', '-q', 'HEAD'], universal_newlines=True).strip()
>         except (subprocess.CalledProcessError, FileNotFoundError):
>             branch_name = ""
>         finally:
>             os.chdir(cur_path)
>     return re.sub("[^A-Za-z0-9]+", "", branch_name).lower()
> ```
>
> Until that change lands, a worktree upload will still read the main checkout's branch. As a
> stop-gap you can temporarily check out the feature branch in the main repo, but that is
> exactly the dance this skill aims to remove. This repo (`android`) **cannot** edit
> `transifex/weblate` — coordinate the one-line change with the Weblate tooling owner.

### Step 1 — Pull latest in the Weblate repo

The `transifex/weblate/` directory is a separate git repository. Pull latest:

```bash
cd transifex/weblate && git pull
```

### Step 2 — Extract new strings from the current branch

Run a diff against `origin/develop` to find newly added string lines (including their comment
descriptions) in the shared strings file. Use `origin/develop` (not local `develop`) as the
base, since the local `develop` ref is often stale:

```bash
git diff origin/develop -- resources/string-resources/src/main/res/values/strings_shared.xml
```

Parse the diff output to extract all added lines (lines starting with `+` that are not `+++`).
These will be `<!-- comment -->` and `<string name="...">...</string>` lines.

If no new strings are found, inform the user and stop — there is nothing to upload.

### Step 2b — Ensure every new string has a description

Every uploaded string must carry a description for translators: the `<!-- comment -->` directly
above the string in `strings_shared.xml` becomes the unit's source-string note in Weblate, and
Step 6f additionally copies it into the unit's editable `explanation` field. The push gate hook
rejects strings that end up in Weblate with neither.

For each new string from Step 2, check that the diff contains a comment line immediately above
its `<string>`/`<plurals>` entry. For any string **without** a comment:

1. Draft a one-line description covering: what the text says/does, where it appears (screen,
   dialog, snackbar…), and the meaning of every placeholder (`%1$s`, `%d`, …). For `<plurals>`,
   note what the quantity refers to.
2. Show the drafted description(s) to the developer and ask for confirmation or edits.
3. Add the confirmed comment above the string in
   `resources/string-resources/src/main/res/values/strings_shared.xml` and commit it to the
   branch — the description is part of the string resource, not upload-only metadata.
4. Carry the comment into the Step 3 `strings.xml` like any other.

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

Once the user confirms, run the upload script from the project root. Pass the feature branch
derived in Step 0 explicitly via the `gitlabBranch` environment variable so the correct
per-branch component slug is used even from a git worktree (see Step 0 prerequisite):

```bash
gitlabBranch="$FEATURE_BRANCH" ./transifex/weblate/lang.sh -a android -u -f strings.xml -c strings_shared
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

### Step 5 — Source a screenshot for each new string (prefer screenshot-test goldens)

Every string uploaded to Weblate **must** have a screenshot showing it in context — it gives
translators the surrounding UI, and the push gate hook denies the push of any string without
a mapped screenshot. **Prefer Compose screenshot-test goldens as the image source**
rather than always asking the developer for an ad-hoc screenshot. A golden is a recorded,
deterministic render of a real composable, so if one already shows the string, reuse it; if
none exists, add a screenshot test, record the golden, and use that.

Work through the sub-steps below **per new string** (a single golden may cover several strings
on the same screen — that's fine, reuse it for all of them). Collect the resulting PNG path(s)
and the strings each one covers; you'll upload and map them in Step 6.

#### Step 5a — Locate the composable that uses each string

Find where each new string is referenced. String resources from `strings_shared.xml` are
resolved through `mega.privacy.android.shared.resources.R` (imported variously as `sharedR`,
`sharedResR`, `SharedResR`, or unaliased). Grep for the `.string.<name>` suffix to catch every
alias form at once:

```bash
grep -rn "\.string\.<string_name>\b" --include=*.kt .
```

Inspect the hits under `src/main/.../*.kt` that call `stringResource(...)` to find the
composable(s). Note: a string is sometimes passed indirectly (a `@StringRes Int` argument, or
resolved in a ViewModel/mapper), so a direct hit may land in a mapper/VM — trace it forward to
the composable that actually renders it. Record the composable name and its module.

#### Step 5b — Check for an existing golden

Screenshot tests live in `<module>/src/screenshotTest/kotlin/<package>/...` and their recorded
reference PNGs live under:

```
<module>/src/screenshotTestDebug/reference/<package-path>/<TestClass>/<TestMethod>_<variant>_<hash>_0.png
```

(The flavored `app` module uses `src/screenshotTestGmsDebug/reference/...` instead.) Each
`@CombinedThemePreviews` method produces two PNGs — `..._1-Dark theme_..._0.png` and
`..._2-Light theme_..._0.png`. **Prefer the light-theme variant** for Weblate — text is more
legible on a light background.

For the composable found in Step 5a, look for a `*ScreenshotTest.kt` in the same module that
invokes it, then check for its recorded PNG under that module's `reference/` directory:

```bash
grep -rln "<ComposableName>" --include=*.kt <module>/src/screenshotTest/
```

**If a golden exists that renders the string**, reuse its light-theme PNG as the screenshot for
that string and skip to Step 6. Read the PNG with the Read tool and confirm the string text is
actually visible before reusing it (the composable may hide it behind a state the test doesn't
exercise).

#### Step 5c — No golden yet: add a screenshot test and record it (capturable components)

If the string appears on a screen or component that can be rendered in a static preview (screen
body, dialog, list row, empty state, banner, etc.), add a screenshot test that renders that
composable, then record the golden.

Add a test class next to the existing screenshot tests in the composable's module, following
the repo convention — `@PreviewTest` + `@CombinedThemePreviews`, wrapped in the theme wrapper:

```kotlin
package <same package as the composable>

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class <ComposableName>ScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun <ComposableName>Default() {
        AndroidThemeForPreviews {
            <ComposableName>(
                // pass whatever state makes the new string visible
            )
        }
    }
}
```

Use `mega.android.core.ui.preview.CombinedThemePreviews` and `AndroidThemeForPreviews` for new
core-ui composables. Older screens that still use `shared.original.core.ui` may need the legacy
`mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews` + `OriginalTheme`
wrapper instead — match the surrounding tests in that module.

Record the golden:

```bash
./gradlew :<module-path>:updateDebugScreenshotTest
```

(For the flavored `app` module the task is `:app:updateGmsDebugScreenshotTest`.) The recorded
PNGs land under the `reference/` path from Step 5b — use the light-theme one for the upload.

> **Environment caveat:** goldens recorded on a local Mac can differ from CI by a few percent
> (font metrics), so a locally recorded PNG may fail CI `validateDebugScreenshotTest` even
> though it renders correctly. That does **not** block the Weblate upload — the local PNG is
> perfectly good as a translator reference. But the new test + golden are committed to the
> branch and become a CI baseline, so after pushing, check the screenshot-validation CI job and
> re-record in the canonical environment if it fails.

#### Step 5d — Fleeting components (snackbars, toasts, tooltips, content descriptions)

Some strings never settle on a static screen: snackbars/toasts animate in from a state-driven
host (a static preview renders them invisible), core-ui tooltips draw in a separate `Popup`
window the screenshot engine cannot capture, and content descriptions have no visual at all.
These still **must** get a screenshot — uploading without one is not an option (the push gate
denies the push). Use the shared helpers in `:core-test`
(`mega.privacy.android.core.test.weblate`), adding
`screenshotTestImplementation(project(":core-test"))` to the module if it is missing:

- **Snackbar/toast strings** — `WeblateSnackbarScreenshot`: renders the screen content with a
  MEGA-styled snackbar pinned at the bottom, exactly as it appears live:
  ```kotlin
  WeblateSnackbarScreenshot(
      snackbarText = stringResource(sharedR.string.my_new_snackbar_message),
      actionLabel = "Undo", // optional
  ) {
      MyScreenContent(...)
  }
  ```
- **Content-description strings** — `WeblateContentDescriptionScreenshot`: renders the
  **whole screen** with an in-hierarchy MEGA tooltip bubble overlaid, its caret pointing at
  the control the text describes:
  ```kotlin
  WeblateContentDescriptionScreenshot(
      description = stringResource(sharedR.string.my_new_content_description),
      tooltipAlignment = Alignment.TopEnd, // where the described control sits
      tooltipOffset = DpOffset(x = (-155).dp, y = 52.dp), // nudge caret under the control
  ) {
      MyScreen(...) // the full screen containing the control
  }
  ```
  The caret follows the alignment's horizontal bias (start/centre/end). Record the golden,
  view it, and adjust `tooltipOffset` until the caret sits under the described control
  before uploading.

Wrap either helper in the usual `@PreviewTest @CombinedThemePreviews` + theme-wrapper test
method (Step 5c) and record the golden as normal.

#### Step 5e — Manual screenshot fallback

If no composable/golden path applies (e.g. the string lives in legacy XML/View UI the
screenshot plugin can't render), fall back to the original behaviour: ask the developer for an
image path. Then read the image with the Read tool, propose which uploaded strings are visible,
and ask the developer to confirm the mapping before proceeding.

### Step 6 — Upload screenshots and map strings

For each PNG collected in Step 5 (a reused golden, a freshly recorded golden, or a manual
screenshot), upload it and map it to the strings it covers.

Read the API config from `transifex/weblate/translate.json` to get `BASE_URL` and `SOURCE_TOKEN`.

> **Note on curl writes:** the repo's bash-guard hook hard-denies direct `curl` write calls
> (`-X POST/PATCH`, `-d`, `-F`). Write each write-call below (6c, 6e, 6f) into a small script
> file (e.g. `transifex/weblate/tmp_weblate_<ticket>.sh`, matching the existing
> `map_screenshot_*.sh` pattern) and execute that script instead of running `curl` inline.

#### Step 6a — Get the branch component slug

The branch component slug follows the pattern: `strings_shared-<sanitized_branch>`

The branch name is sanitized by stripping all non-alphanumeric characters and lowercasing,
matching the logic in `transifex/weblate/python/android.py`:
```python
re.sub("[^A-Za-z0-9]+", "", branch_name).lower()
```

Use the feature branch derived in Step 0 (`$FEATURE_BRANCH`) — **not** `git branch --show-current`
run from an arbitrary directory — so the slug is correct from a worktree:
```bash
SANITIZED=$(echo "$FEATURE_BRANCH" | sed 's/[^A-Za-z0-9]//g' | tr '[:upper:]' '[:lower:]')
COMPONENT_SLUG="strings_shared-${SANITIZED}"
```

Example: `lh/AND-23288-move-ads-free-intro-to-shared-ads` → `strings_shared-lhand23288moveadsfreeintrotosharedads`

#### Step 6b — Resize and rename the screenshot

Weblate rejects images that are too large. Scale down to max 1200px. This applies to golden
PNGs too (recorded goldens render at `1080×2340` and should be scaled down):

Also rename the screenshot to match the branch context for easy identification in Weblate.
Extract the Jira ticket ID from the branch name (e.g., `AND-23288` from
`lh/AND-23288-move-ads-free-intro-to-shared-ads`) and use it as the screenshot name:

```bash
TICKET=$(echo "$FEATURE_BRANCH" | grep -oE 'AND-[0-9]+')
SCREENSHOT_NAME="${TICKET:-$(echo "$FEATURE_BRANCH" | sed 's|.*/||')}.png"
sips -Z 1200 "<screenshot_path>" --out /tmp/weblate_screenshot.png
```

Example: branch `lh/AND-23288-move-ads-free-intro-to-shared-ads` → screenshot name `AND-23288.png`

If there are multiple screenshots (several goldens, or one per screen), append a counter:
`AND-23288_1.png`, `AND-23288_2.png`.

#### Step 6c — Upload the screenshot

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

#### Step 6d — Find the unit IDs for the uploaded strings

For each string name the screenshot covers (from Step 5), look up its unit ID using the
`context:=` query:

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

#### Step 6e — Map strings to the screenshot

For each unit ID found, associate it with the screenshot:

```bash
curl -s -X POST \
    -H "Authorization: Token <SOURCE_TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{"unit_id": <unit_id>}' \
    "<BASE_URL>/screenshots/<screenshot_id>/units/"
```

### Step 6f — Set the translator-facing explanation for every uploaded string

The XML comment only reaches the unit's read-only source note. Weblate also has an editable
`explanation` field that is shown more prominently to translators — set it for **every**
uploaded string (all of them, not just the screenshot-mapped ones).

For each uploaded string, reuse the unit ID from Step 6d (or look it up with the same
`context:=` query), then PATCH the unit:

```bash
curl -s -X PATCH \
    -H "Authorization: Token <SOURCE_TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{"explanation": "<description>"}' \
    "<BASE_URL>/units/<unit_id>/"
```

The explanation text is the string's description from Step 2b (the XML comment, trimmed),
expanded if needed so placeholders and plural quantities are spelled out. Escape any double
quotes for the JSON body. Verify each response echoes the explanation back; report any
failures in Step 7 — the push gate accepts a unit note as a fallback, but the explanation
should normally be present.

### Step 7 — Confirm

Report the result to the user:
- List the strings that were uploaded
- Show the upload script output (success/failure)
- Confirm the explanation was set for each string (Step 6f), noting any that were drafted
  fresh in Step 2b and any PATCH failures
- For each string, note the screenshot source: reused golden, newly added screenshot test
  (name the test + golden path), fleeting-component example, or manual image
- If a screenshot was uploaded: show the screenshot name and how many strings were mapped to it
- If new screenshot tests/goldens were added, remind the user they are committed to the branch
  and must pass the CI screenshot-validation job (re-record in the canonical environment if it
  fails — see the Step 5c environment caveat)
- Show the URL, for example: https://translate.developers.mega.co.nz/projects/android/{COMPONENT_SLUG}/
