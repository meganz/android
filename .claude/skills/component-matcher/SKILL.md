---
name: component-matcher
description: >
  Analyze a UI screenshot and identify which MEGA core-ui library components
  match the visible elements. Extracts component metadata from the sources.jar
  and performs text + visual matching against component screenshots.
triggers:
  - /component-matcher
  - match components
  - identify components
  - which core-ui components
  - component lookup
  - match components from figma
  - figma component matcher
---

# Component Matcher Skill

Analyze a UI screenshot to identify which MEGA core-ui Compose library components can be used for the visible UI elements. Uses a two-phase approach: text-based narrowing with `llms.txt`, then visual comparison with component screenshots extracted from the `sources.jar`.

## Usage

```
/component-matcher ./designs/home-screen.png
/component-matcher /tmp/screenshot.png --categories button,toolbar,list
/component-matcher https://figma.com/design/abc123/MyApp?node-id=42-1337
/component-matcher https://figma.com/design/abc123/branch/def456/MyApp --categories button,toolbar
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| Image source | Path to a local screenshot **or** a Figma URL to capture (required, first positional arg) | `./designs/home.png` or `https://figma.com/design/abc123/MyApp?node-id=42-1337` |
| `--categories <list>` | Comma-separated category filter to limit matching scope (optional) | `--categories button,toolbar` |

---

## Execution Steps

### Step 0 — Resolve the core-ui sources.jar

1. Read the core-ui version from the version catalog:
   ```
   Grep: pattern "mega-core-ui = " in gradle/catalogs/lib.versions.toml
   ```
   Extract the version string from the `[versions]` section (format: `mega-core-ui = "X.Y.Z"`).

2. Locate the sources.jar in the Gradle cache:
   ```bash
   find ~/.gradle/caches/modules-2/files-2.1/mega.android.core/ui/{version}/ -name "*-sources.jar" 2>/dev/null
   ```
   The jar lives inside a hash-named subdirectory, so `find` is necessary.

3. Store the resolved path as `SOURCES_JAR` for subsequent steps.

4. **If not found**, output this message and **stop**:
   ```
   The core-ui sources.jar for version {version} was not found in the Gradle cache.
   Please run: ./gradlew :app:dependencies --configuration debugCompileClasspath
   Then retry /component-matcher.
   ```

### Step 1 — Acquire the user's input image

**1a. Determine the input type:**

Inspect the first positional argument:
- If it starts with `http` and contains `figma.com/design/`, treat it as a **Figma URL** → go to **1b**.
- Otherwise, treat it as a **local file path** → go to **1c**.

**1b. Figma URL — fetch screenshot via MCP:**

1. Parse the URL to extract `fileKey` and `nodeId`:
   - Standard URL: `figma.com/design/:fileKey/:fileName?node-id=:nodeId`
     - `fileKey` = the path segment after `/design/`
     - `nodeId` = the `node-id` query parameter, with `-` converted to `:` (e.g., `42-1337` → `42:1337`)
   - Branch URL: `figma.com/design/:fileKey/branch/:branchKey/:fileName`
     - Use `:branchKey` as the `fileKey`
     - `nodeId` = the `node-id` query parameter (same `-` to `:` conversion)

2. **Validate** that both `fileKey` and `nodeId` were extracted. If `nodeId` is missing (no `node-id` query parameter in the URL), inform the user:
   ```
   The Figma URL must include a node-id parameter (e.g., ?node-id=42-1337).
   Please select a specific frame or component in Figma, copy its link, and retry.
   ```
   **Stop execution.**

3. Call the Figma MCP tool to capture the screenshot:
   ```
   mcp__plugin_figma_figma__get_screenshot(fileKey: "<extracted>", nodeId: "<extracted>")
   ```
   The tool returns the screenshot rendered visually in the conversation, the same as a `Read` on a local PNG.

4. **If the MCP call fails** (e.g., tool not available, auth error, invalid file/node), output:
   ```
   Failed to fetch screenshot from Figma. Possible causes:
   - The Figma MCP server is not connected (run /plugin to connect it)
   - The fileKey or nodeId is invalid
   - You do not have access to this Figma file

   You can alternatively export the frame as a PNG and use a local file path:
     /component-matcher ./exported-frame.png
   ```
   **Stop execution.**

5. Store the original Figma URL as `IMAGE_SOURCE` for the report.

**1c. Local file path — read from disk:**

1. Use the `Read` tool to view the image at the provided path. Claude Code's multimodal capability will render the image visually.
2. Store the file path as `IMAGE_SOURCE` for the report.

**1d. Visual inventory (applies to both input types):**

1. Perform an initial visual analysis and produce a **numbered inventory** of all distinct UI elements visible in the screenshot. For example:
   ```
   1. Top app bar with back arrow and title "Settings"
   2. Toggle list items (3 rows with title, subtitle, trailing toggle)
   3. Divider between sections
   4. Primary filled button at bottom ("Save")
   5. Inline warning banner below the toggle section
   ```

2. Present this inventory to the user before proceeding. This ensures alignment on what elements are being matched.

### Step 2 — Text-based category narrowing with llms.txt

1. Extract the compact component catalog from the jar:
   ```bash
   unzip -p "$SOURCES_JAR" META-INF/mega.android.core.ui/llms.txt
   ```

2. Read the full `llms.txt` content (~311 lines). It is organized by category with one-line component summaries including parameter names and descriptions.

3. For each UI element identified in Step 1, select **1-5 candidate components** based on:
   - Component name and description matching the visual element
   - Parameter signatures (e.g., does it accept `title`, `subtitle`, `leadingElement`, `trailingElement`?)
   - Category relevance

4. If `--categories` was specified, restrict candidates to only those categories.

5. Build a candidate shortlist. This typically reduces 124 possible screenshots down to 5-15 relevant candidates.

**Available categories in llms.txt:**
badge, banner, button, card, card/plans, checkbox, chip, dialogs, divider, dropdown, empty, fab, general, image, indicators, inputfields, label, list, navigation, profile, prompt, scrollbar, scrollbar/fastscroll, settings, sheets, slider, snackbar, state, surface, tabs, text, thumbnail, toggle, toolbar, tooltip/component, tooltip/popup/interactive, tooltip/popup/simple

### Step 3 — Visual comparison with candidate screenshots

For each candidate component that has a screenshot in the library:

1. Determine the screenshot path inside the jar. The naming convention is:
   ```
   META-INF/mega.android.core.ui/screenshots/mega/android/core/ui/screenshots/{Category}ScreenshotsKt/{ComponentName}_Screenshot_748aa731_0.png
   ```

   **Category-to-screenshot-directory mapping:**

   | llms.txt category | Screenshot directory |
   |-------------------|---------------------|
   | banner | BannerScreenshotsKt |
   | button | ButtonScreenshotsKt |
   | card | CardScreenshotsKt |
   | card/plans | CardPlansScreenshotsKt |
   | checkbox | CheckboxScreenshotsKt |
   | chip | ChipScreenshotsKt |
   | dialogs | DialogsScreenshotsKt |
   | divider | DividerScreenshotsKt |
   | fab | FabScreenshotsKt |
   | general | GeneralScreenshotsKt |
   | indicators | IndicatorsScreenshotsKt |
   | inputfields | InputfieldsScreenshotsKt |
   | list | ListScreenshotsKt |
   | navigation | NavigationScreenshotsKt |
   | profile | ProfileScreenshotsKt |
   | prompt | PromptScreenshotsKt |
   | scrollbar/fastscroll | ScrollbarFastscrollScreenshotsKt |
   | sheets | SheetsScreenshotsKt |
   | slider | SliderScreenshotsKt |
   | snackbar | SnackbarScreenshotsKt |
   | state | StateScreenshotsKt |
   | surface | SurfaceScreenshotsKt |
   | tabs | TabsScreenshotsKt |
   | text | TextScreenshotsKt |
   | thumbnail | ThumbnailScreenshotsKt |
   | toggle | ToggleScreenshotsKt |
   | toolbar | ToolbarScreenshotsKt |

   **Categories with NO screenshots** (text-only matching): badge, dropdown, empty, image, label, scrollbar (non-fastscroll), settings, tooltip/component, tooltip/popup/interactive, tooltip/popup/simple

2. Extract only the candidate screenshot PNGs to a temp directory:
   ```bash
   mkdir -p /tmp/core-ui-match
   unzip -j "$SOURCES_JAR" "META-INF/mega.android.core.ui/screenshots/mega/android/core/ui/screenshots/{Category}ScreenshotsKt/{ComponentName}_Screenshot_748aa731_0.png" -d /tmp/core-ui-match/
   ```
   Repeat for each candidate. Only extract screenshots for the shortlisted candidates (typically 5-15), **never all 124**.

3. Use the `Read` tool to view each extracted candidate screenshot PNG.

4. Compare each candidate screenshot against the corresponding region in the user's input image. Assess:
   - **Visual similarity**: layout, shape, spacing, element arrangement
   - **Component type accuracy**: is it actually a button, or a card that looks like one?
   - **Configuration match**: which variant/overload best fits?

5. Assign a confidence level to each match:
   - **High**: near-identical visual match, clearly the right component
   - **Medium**: correct component type but different configuration or styling
   - **Low**: plausible match but uncertain, could be custom

6. For categories without screenshots, matching is text-only. Note this in the output: "No library screenshot available for visual comparison."

### Step 4 — Fetch full signatures for confirmed matches

For each high or medium confidence match:

1. Extract the detailed component reference:
   ```bash
   unzip -p "$SOURCES_JAR" META-INF/mega.android.core.ui/llms-full.txt
   ```

2. Locate the `### {ComponentName}` section in `llms-full.txt` for the matched component.

3. Extract the full `@Composable fun` signature with all parameters and default values, plus any usage example.

### Step 5 — Generate structured report

Output the report directly to the user in this format:

```markdown
# Component Matching Report

**Input:** {IMAGE_SOURCE}
**core-ui Version:** {version}

## Summary

| # | UI Element | Matched Component | Category | Confidence |
|---|-----------|-------------------|----------|------------|
| 1 | Top app bar | MegaTopAppBar | toolbar | High |
| 2 | Toggle list items | SettingsToggleItem | settings | Medium |
| 3 | Divider | SubtleDivider | divider | High |
| 4 | Primary button | PrimaryFilledButton | button | High |
| 5 | Warning banner | InlineWarningBanner | banner | High |
| 6 | Custom header | No match | - | - |

## Detailed Matches

### 1. Top app bar -> MegaTopAppBar (toolbar) -- HIGH

[Show the library screenshot via Read tool output]

**Function Signature:**
[Full @Composable fun signature from llms-full.txt]

**Notes:** The top bar shows a back arrow and title, matching `AppBarNavigationType.Back`.

---

### 6. Custom header -> No Match

**Notes:** This header layout with a custom illustration does not correspond to any existing core-ui component. You will need a custom composable.

---

## Unmatched Elements (Custom Components Needed)

- Custom header with illustration — consider using `MegaScaffold` as the container
```

For composite elements (e.g., a list item with a toggle), identify each contributing component separately:
```
### 2. Toggle list items -> SettingsToggleItem (settings) -- MEDIUM

**Notes:** SettingsToggleItem provides title + subtitle + toggle in a single component.
Alternatively, this could be composed from `TwoLineListItem` (list) + `Toggle` (toggle)
if more layout control is needed.
```

### Step 6 — Cleanup

Remove temporary screenshot files:
```bash
rm -rf /tmp/core-ui-match/
```

---

## Guidelines

1. **Two-phase matching is mandatory** — always narrow with `llms.txt` text descriptions first, then extract only candidate screenshots for visual comparison. Never extract all 124 screenshots.
2. **Dynamic version resolution** — always read the version from `gradle/catalogs/lib.versions.toml` at runtime. Never hardcode the version or jar path.
3. **Composite patterns** — when a UI element appears to combine multiple core-ui components (e.g., a list item with a toggle trailing element), identify each contributing component separately and note the composition.
4. **No match is a valid answer** — not every UI element will have a core-ui equivalent. Explicitly call out elements that need custom composables, and suggest which core-ui components might serve as building blocks.
5. **Confidence transparency** — always state the confidence level and explain why. High = near-identical visual match; Medium = correct component type, different configuration; Low = plausible but uncertain.
6. **Minimal temp files** — extract only candidate PNGs (5-15), not all. Clean up `/tmp/core-ui-match/` when done.
7. **Overload awareness** — many components have multiple overloads (e.g., `BasicDialog` has 5, `MegaIcon` has 9). Recommend the specific overload whose parameters best match what is visible in the screenshot.
8. **Standard Compose vs core-ui** — recognize standard Android/Compose components (`Column`, `LazyColumn`, `Scaffold`, `Text`) and note they are platform primitives, not core-ui components.
9. **Figma MCP fallback** — when a Figma URL is provided but the MCP tool is unavailable or fails, always suggest the local-file-path alternative. Never silently skip the image acquisition step.
