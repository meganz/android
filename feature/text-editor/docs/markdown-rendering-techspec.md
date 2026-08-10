# Tech Spec — Markdown Rendering in the Compose Text Editor

**Ticket:** AND-24001 — Text Editor: render Markdown files with formatting in view mode
**Module:** `feature/text-editor/text-editor` (+ `feature/text-editor/text-editor-snowflake-components`)
**Last updated:** 2026-06-24

> **Status:** Design / pre-implementation. No production code written yet. This spec is for team review before coding.

> **Revision (2026-06-25) — the implemented design differs from the original proposal below.**
> The key facts that changed are summarised here; the rest of the document is kept for history.
>
> - **In-house renderer, no third-party UI library.** Markdown is parsed with the existing
>   `org.commonmark:commonmark` (plus `commonmark-ext-gfm-tables` for GFM tables) and rendered by
>   our own Compose code (`MarkdownNode` / `MarkdownPreview`). The `multiplatform-markdown-renderer`
>   (mikepenz) library proposed in §3 was **not** adopted.
> - **Preview-only — no Preview/Source toggle.** Markdown opens directly in the formatted preview in
>   View mode; tapping Edit shows the raw chunked source. There is no in-view toggle and no
>   `contentView` state.
> - **No size guard or fallback.** Parsing runs on a background dispatcher and the preview is
>   virtualized in a `LazyColumn`, so large files render without a size cap; over-long single lines
>   are split into multiple `Text`s to avoid native text-measurement ANRs.
> - **Precise Preview↔Edit scroll sync** maps scroll offset to logical line via the real
>   `TextLayoutResult`, not a fixed line-height estimate, so both directions land on the exact line.
> - Shipped as three stacked MRs: AND-24015 (deps + helper + flag, !16396), AND-24016 (state + VM,
>   !16412), AND-24017 (renderer + UI, !16413).

## 1. Background & problem

The legacy `app/textEditor` already renders Markdown: it detects `.md` by extension
(`TextEditorViewModel.isMarkDownFile`), converts CommonMark → HTML via
`org.commonmark:commonmark:0.28.0` (chunking the conversion at 100 KB), and shows the
result in a `WebView`. Edit mode stays raw text.

The new Compose editor (`feature/text-editor`) has **no `.md` awareness** — every file,
including Markdown, renders as plain `BasicText` chunks. This ticket ports Markdown viewing
to the Compose editor, Compose-native (no WebView).

### Why our loading approach makes off-the-shelf rendering non-trivial

The Compose editor is built around **streamed, chunked, virtualized** content:

- Content streams in as 500-line batches (`GetTextContentForTextEditorUseCase` et al.).
- View mode slices `fullContentLines` into chunks (1000 lines, or ≤ 50 KB chars under the
  `TextEditorLongLineChunking` flag) via `buildChunkBoundaries()`.
- Each chunk renders as an independent `BasicText` inside a `LazyColumn`, with a
  line-number gutter and fast scrollbar (`TextEditorContent.kt → ReadOnlyChunkItem`).

**Markdown cannot be rendered per-chunk.** A code fence, list, table, blockquote, or
reference-style link routinely spans a chunk boundary; feeding a renderer half of a block
produces garbage. A Markdown renderer needs the **whole document** (or whole block
structures) as one unit — which directly opposes the chunk-virtualize design that is the
editor's reason for existing.

The mitigating fact: the ViewModel already accumulates the full text in `fullContentLines`,
so reconstructing the complete string for a renderer is cheap.

## 2. Goals / non-goals

**Goals (this ticket):**
- Detect `.md` / `.markdown` files.
- Render a formatted, **read-only preview** in view mode (headings, bold/italic, ordered &
  unordered lists, links, inline code, code blocks, blockquotes, horizontal rules, tables).
- A **Preview ⇄ Source** toggle. Source = the existing chunked raw-text view (line numbers,
  fast scroll, edit) unchanged.
- A **large-file size guard**: above a threshold, skip rendering and fall back to raw view.
- Theme the rendered output with **DSTokens** (light/dark).
- Gate the whole feature behind a **dedicated feature flag** (see §4.6).

**Non-goals (follow-ups):**
- WYSIWYG Markdown editing — edit mode stays raw source.
- **Remote image loading** — disabled for v1 (see §6 Security). Tracked separately.
- Syntax highlighting inside code fences.
- **Text selection / copy in Preview** is *nice-to-have, not blocking* (see §3): if the
  renderer can't cooperate with `SelectionContainer`, v1 ships without it and users select
  via the Source view.

## 3. Library choice — `multiplatform-markdown-renderer` (mikepenz)

Pure-Compose, CommonMark-based, actively maintained. Latest ~`0.39.x` targets Compose
1.10 — compatible with our `compose-bom = 2026.05.00` and `kotlin = 2.3.20`.

**Rejected alternatives:**
- **Compose RichText** (halilibo) — pure-Compose/CommonMark but less actively maintained.
- **`compose-markdown`** (jeziellago) — wraps Markwon into an `AndroidView`-hosted
  `TextView`; not Compose-native, defeats the pure-Compose decision.

**Expected artifacts** (Android; confirm exact coords + version at add-time, add to
`gradle/catalogs/lib.versions.toml`):
- `com.mikepenz:multiplatform-markdown-renderer-android`
- `com.mikepenz:multiplatform-markdown-renderer-m3-android` (Material 3 theming hooks)
- ~~`...-coil3-android`~~ **omitted in v1** — no remote image loading.

Entry point: `Markdown(content, colors, typography, …)`, themable via `markdownColor()` /
`markdownTypography()` builders mapped to DSTokens + core-ui text styles.

> **To validate during implementation** (the "vet" items behind choosing a 3rd-party lib):
> (1) **text selection** — does `Markdown(...)` cooperate with `SelectionContainer`? This is
> *nice-to-have, not a gating requirement* (§2): if it doesn't work, v1 ships without
> in-Preview selection and users copy via Source. (2) full DSTokens coverage via the
> color/typography builders. (3) the char threshold where rendering janks (sets the size
> guard). (4) link-tap behaviour (open via `LocalUriHandler` → browser; relative/anchor links
> that aren't `http(s)` are no-ops in v1).

## 4. Architecture & integration points

```
                 ┌──────────────────────────────────────────────┐
                 │            TextEditorComposeViewModel          │
                 │  fullContentLines (whole content in memory)    │
                 │  + isMarkdown (ext && flag on)                 │
                 │  + contentView: Preview | Source (toggle)      │
                 └───────────────┬────────────────────────────────┘
                                 │
        flag on && mode == View && isMarkdown && contentView == Preview
                 ┌───────────────┴───────────────┐
                 │ render path A: MARKDOWN PREVIEW │   render path B: RAW (existing)
                 ▼                                 ▼
   ┌─────────────────────────────┐   ┌─────────────────────────────────────┐
   │  MarkdownPreview composable │   │  TextEditorContent (LazyColumn of     │
   │  full string from           │   │  BasicText / BasicTextField chunks,   │
   │  fullContentLines           │   │  line numbers, fast scroll) UNCHANGED │
   │  → Markdown(...) in its own  │   └─────────────────────────────────────┘
   │  scroll container            │
   │  size-guard: > THRESHOLD →   │
   │  fall back to path B         │
   └─────────────────────────────┘
```

### 4.1 Detection
- Derive `isMarkdown: Boolean` from the file extension (`md` / `markdown`, case-insensitive).
  Reuse the existing file-type path rather than an ad-hoc `endsWith`: the domain already has
  `FileTypeInfo` / `TextFileTypeInfo` (carries `extension`) and `GetFileTypeInfoByNameUseCase`.
  There is **no** dedicated `MarkdownFileTypeInfo`, so add a small shared helper
  (`fun String.isMarkdownExtension()` or equivalent) used both here and anywhere else that
  needs it — single source of truth for the extension set.
- Set once at load, exposed on `TextEditorComposeUiState`.
- A Markdown file (with the flag on) defaults to **Preview** on each fresh open.

### 4.2 State
- `TextEditorComposeUiState` gains `isMarkdown: Boolean` and a `contentView` enum
  (`Preview` | `Source`) — only meaningful when `isMarkdown && mode == View`.
- Edit/Create modes are unaffected (always raw source).
- `contentView` is held in `SavedStateHandle` so the choice survives configuration changes
  (rotation), but resets to `Preview` on a fresh open. No cross-session persistence in v1.
- Preview and Source each keep their **own independent scroll position** within a session; a
  toggle does not try to map scroll between the rendered tree and the raw line/char model.

### 4.3 Rendering (UI layer only — ViewModel stays text-only)
- New `MarkdownPreview` composable in `text-editor-snowflake-components`, sibling to
  `TextEditorContent`. Takes the **full document string** + themed colors/typography.
- `TextEditorScreen` chooses path A vs B based on `isMarkdown` + `contentView` + `mode`.
- Markdown parsing/rendering happens entirely in the UI layer; `getChunkText()` and the
  chunk machinery are untouched.

### 4.4 Size guard
- Reconstructing the full string is cheap, but rendering a `.md` as one Compose tree risks
  ANR/OOM — the exact case chunking protects against.
- **Two independent limits, both must pass for Preview to render:**
  1. **Total size** — total characters ≤ `MARKDOWN_PREVIEW_MAX_CHARS` (starting value
     200 000; tune with the perf check).
  2. **Longest single line** — the longest logical line ≤ `CHUNK_MAX_CHARS` (50 000, reused
     from the existing long-line cap). This is the lesson of **AND-23707** ("Fix ANR in text
     editor caused by native text measurement on very long lines"): a *single* very long line
     (minified JSON, base64) blocks the main thread in `MeasuredText.nBuildMeasuredText`. A
     total-size-only guard misses this — a 150 KB file that is one 150 KB line passes the
     total check yet still ANRs. The chunked Source path already splits long lines per
     AND-23707 (`buildChunkBoundaries` + `CHUNK_MAX_CHARS`); the un-chunked Preview cannot, so
     it must refuse such files.
- When either limit is exceeded, `MarkdownPreview` is skipped and the raw chunked Source view
  renders instead (which handles long lines safely). The toggle still lets the user force
  Source.

### 4.5 Toggle UI
- Flips `contentView` between Preview and Source.
- **Component**: the editor already models top/bottom bar actions
  (`TextEditorTopBarAction`, `MegaFloatingToolbar`, `MegaTopAppBar`). A top-bar icon toggle
  action is the natural fit. Reusable two-state controls (`Tabs`, `MegaChip`/`ChipBar`,
  `ToggleMegaButton`) exist in `shared.original.core.ui` (the `:app` library) — **not** the
  snowflake `mega.android.core.ui` this module uses. Confirm a segmented/tab control in the
  snowflake core-ui (sources.jar); otherwise use a top-bar icon toggle. Final placement is a
  design call.

### 4.6 Feature flag
- The whole feature is gated behind a **new dedicated flag** (e.g.
  `TextEditorMarkdownRendering`), resolved at init like the existing
  `TextEditorLongLineChunking`.
- **Flag off** → `.md` behaves exactly as today (raw chunked view, no detection, no toggle).
  `isMarkdown` is forced `false` so no Preview path or toggle is ever shown.
- **Flag on** → detection + Preview default + toggle as described above.
- Verify the flag-on UI on a **debug** build (QA's runtime-override datastore short-circuits
  the real flag path).

### 4.7 Preview ⇄ Edit transition & read-through
- Preview keeps the **Edit** affordance. Edit → switches to raw **Source** edit (the existing
  chunked `BasicTextField` path). On save or exit-edit → returns to **Preview**.
- Read-through progress restore (`restoreScrollIndex`, `TextEditorReadThrough`) stays bound to
  the **Source/raw** path only — it is chunk-index based and can't map onto the rendered tree.
  Preview opens at the top; its in-session scroll is remembered per §4.2.

## 5. Affected modules / files

- `domain/`
  - small `isMarkdownExtension()` helper (single source of truth for the `md`/`markdown`
    extension set); a `contentView` enum if we keep it in domain (or keep in presentation).
  - feature-flag entry for `TextEditorMarkdownRendering` (wherever `TextEditorLongLineChunking`
    is declared).
- `feature/text-editor/text-editor`
  - `presentation/TextEditorComposeViewModel.kt` — flag resolve + detection + `contentView`
    state/toggle.
  - `presentation/model/TextEditorComposeUiState.kt` — `isMarkdown`, `contentView`.
  - `presentation/TextEditorScreen.kt` — path A/B selection, toggle action wiring.
  - `presentation/model/TextEditorTopBarAction.kt` — toggle action (if top-bar).
- `feature/text-editor/text-editor-snowflake-components`
  - new `MarkdownPreview.kt` — the rendered view + DSTokens theme mapping.
- `gradle/catalogs/lib.versions.toml` — markdown renderer artifacts.

## 6. Security note (zero-knowledge product)

**Remote images are disabled in v1.** A malicious `.md` with `![](http://attacker/x.png)`
would fire a network beacon on open, revealing that the user viewed the file — a real
exposure for a zero-knowledge product (legacy's WebView had the same gap). v1 omits the
coil3 artifact and shows alt-text/placeholder for images. Loading images (local cache only,
or via an explicit user gesture) is a gated follow-up.

## 7. Testing notes

- **ViewModel unit tests**: `.md` / `.markdown` detection (case-insensitive) via the shared
  helper; **flag off ⇒ `isMarkdown == false`** (no Preview/toggle); flag on ⇒ default
  `contentView == Preview` for `.md` in View mode; toggle flips Preview ⇄ Source; size-guard
  forces Source when **total chars** exceed the cap **and** when a **single line** exceeds
  `CHUNK_MAX_CHARS` (the AND-23707 long-line case); non-`.md` files unaffected either way.
- **Manual / device** (debug build for the flag): headings, bold/italic, lists, links, inline
  code, code fences, blockquotes, tables render; link tap opens browser; non-`http(s)` links
  are no-ops; remote-image Markdown shows no network call (verify no image load); a large `.md`
  (> total cap) **and** a `.md` with one very long line (> 50 KB single line, e.g. minified
  JSON — the AND-23707 case) both fall back to raw Source without ANR; light/dark theming;
  rotation preserves Preview/Source choice while fresh open defaults to Preview;
  Preview→Edit→save returns to Preview; selection works *if* the lib supports it, else copy
  via Source.

## 8. Open questions

- Snowflake core-ui: is there a segmented/tab toggle, or do we use a top-bar icon action?
  (Verify against the `mega.android.core.ui` sources.jar.)
- Exact size-guard threshold (set after the perf spike).

### Resolved (2026-06-24 brainstorm)
- **Feature flag** — yes, a new dedicated flag (§4.6).
- **Selection in Preview** — nice-to-have, not blocking (§2, §3).
- **Preview ⇄ Edit** — Preview keeps Edit; edits raw Source; returns to Preview (§4.7).
- **Scroll across toggle** — independent per view; read-through restore stays on Source (§4.2, §4.7).
- **Detection source** — reuse the file-type/extension path via a shared `isMarkdownExtension()`
  helper; no dedicated `MarkdownFileTypeInfo` (§4.1).
- **Preview/Source persistence** — survives config change via `SavedStateHandle`, resets to
  Preview on fresh open; no cross-session persistence in v1 (§4.2).
