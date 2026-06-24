# Markdown Rendering (AND-24001) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render `.md`/`.markdown` files in the Compose text editor as a formatted, read-only preview with a Preview⇄Source toggle, gated behind a feature flag, edit stays raw source.

**Architecture:** A separate whole-document render path (`MarkdownPreview`, pure Compose via the mikepenz renderer) sits beside the existing chunked `TextEditorContent`. The ViewModel resolves a feature flag + exposes the full content string with a size guard; the Screen picks the path from `isMarkdown` + `contentView` + edit state. Detection is a pure `String.isMarkdownFile()` helper; `isMarkdown` is derived reactively in the UI state so it tracks rename/chat/link filename changes for free.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.05.00), Hilt (assisted VM), `com.mikepenz:multiplatform-markdown-renderer` (m3, Android), JUnit5 + Mockito + Turbine + Truth.

**Worktree:** `.claude/worktrees/AND-24001` (branch `juh/AND-24001-text-editor-markdown-rendering`). Run all commands from there.

**Companion spec:** [markdown-rendering-techspec.md](./markdown-rendering-techspec.md)

---

## File Structure

**Create:**
- `domain/src/main/kotlin/mega/privacy/android/domain/extension/MarkdownFileExtension.kt` — `String.isMarkdownFile()` helper (single source of truth for the `md`/`markdown` extension set).
- `domain/src/test/kotlin/mega/privacy/android/domain/extension/MarkdownFileExtensionTest.kt` — its test.
- `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorContentView.kt` — `Preview | Source` enum.
- `feature/text-editor/text-editor-snowflake-components/src/main/java/mega/privacy/android/feature/texteditor/components/MarkdownPreview.kt` — the rendered view + DSTokens theme mapping.

**Modify:**
- `gradle/catalogs/lib.versions.toml` — add the renderer artifacts.
- `feature/text-editor/text-editor-snowflake-components/text-editor-snowflake-components.gradle.kts` — depend on the renderer.
- `domain/src/main/kotlin/mega/privacy/android/domain/featuretoggle/ApiFeatures.kt` — add `TextEditorMarkdownRendering` flag.
- `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorComposeUiState.kt` — add `isMarkdownEnabled`, `contentView`, derived `isMarkdown`.
- `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorComposeViewModel.kt` — resolve flag, `toggleContentView()`, `getMarkdownPreviewContent()`.
- `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorTopBarAction.kt` — add toggle action.
- `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorScreen.kt` — render-path branch + toggle wiring.
- `feature/text-editor/text-editor/src/test/java/mega/privacy/android/feature/texteditor/presentation/TextEditorComposeViewModelTest.kt` — VM tests.
- string resources (`strings_shared.xml`) — toggle action label.

---

## Task 1: Add the Markdown renderer dependency

**Files:**
- Modify: `gradle/catalogs/lib.versions.toml`
- Modify: `feature/text-editor/text-editor-snowflake-components/text-editor-snowflake-components.gradle.kts`

- [ ] **Step 1: Add the version + libraries to the catalog**

In `gradle/catalogs/lib.versions.toml`, under `[versions]` (near `commonmark-java`):

```toml
multiplatform-markdown-renderer = "0.39.2"
```

Under `[libraries]` (near `commonmark-java`):

```toml
multiplatform-markdown-renderer = { module = "com.mikepenz:multiplatform-markdown-renderer-android", version.ref = "multiplatform-markdown-renderer" }
multiplatform-markdown-renderer-m3 = { module = "com.mikepenz:multiplatform-markdown-renderer-m3-android", version.ref = "multiplatform-markdown-renderer" }
```

- [ ] **Step 2: Depend on it from the snowflake-components module**

In `feature/text-editor/text-editor-snowflake-components/text-editor-snowflake-components.gradle.kts`, in the `dependencies { }` block:

```kotlin
implementation(lib.multiplatform.markdown.renderer)
implementation(lib.multiplatform.markdown.renderer.m3)
```

- [ ] **Step 3: Verify the dependency resolves**

Run: `./gradlew :feature:text-editor:text-editor-snowflake-components:dependencies --configuration debugRuntimeClasspath | grep multiplatform-markdown`
Expected: both artifacts listed at `0.39.2`. If resolution fails, the artifact name/version is off — check the latest on Maven Central (`com.mikepenz` group) and adjust the coordinates; the `-android` suffix is the Android KMP target.

- [ ] **Step 4: Commit**

```bash
git add gradle/catalogs/lib.versions.toml feature/text-editor/text-editor-snowflake-components/text-editor-snowflake-components.gradle.kts
git commit -m "AND-24001 Add multiplatform-markdown-renderer dependency"
```

---

## Task 2: `String.isMarkdownFile()` detection helper (TDD)

**Files:**
- Create: `domain/src/main/kotlin/mega/privacy/android/domain/extension/MarkdownFileExtension.kt`
- Test: `domain/src/test/kotlin/mega/privacy/android/domain/extension/MarkdownFileExtensionTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package mega.privacy.android.domain.extension

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MarkdownFileExtensionTest {

    @Test
    fun `test that isMarkdownFile returns true for md extension`() {
        assertThat("README.md".isMarkdownFile()).isTrue()
    }

    @Test
    fun `test that isMarkdownFile returns true for markdown extension`() {
        assertThat("notes.markdown".isMarkdownFile()).isTrue()
    }

    @Test
    fun `test that isMarkdownFile is case insensitive`() {
        assertThat("READ.MD".isMarkdownFile()).isTrue()
        assertThat("a.Markdown".isMarkdownFile()).isTrue()
    }

    @Test
    fun `test that isMarkdownFile returns false for non markdown files`() {
        assertThat("notes.txt".isMarkdownFile()).isFalse()
        assertThat("script.mdx".isMarkdownFile()).isFalse()
        assertThat("noextension".isMarkdownFile()).isFalse()
        assertThat("".isMarkdownFile()).isFalse()
        assertThat("md".isMarkdownFile()).isFalse()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :domain:test --tests "mega.privacy.android.domain.extension.MarkdownFileExtensionTest"`
Expected: FAIL — unresolved reference `isMarkdownFile`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package mega.privacy.android.domain.extension

/** Markdown file extensions recognised by the text editor. */
private val MARKDOWN_EXTENSIONS = setOf("md", "markdown")

/**
 * Returns true when this file name has a Markdown extension (`.md` / `.markdown`),
 * case-insensitive. Single source of truth for Markdown detection.
 */
fun String.isMarkdownFile(): Boolean =
    substringAfterLast('.', "").lowercase() in MARKDOWN_EXTENSIONS &&
        contains('.')
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :domain:test --tests "mega.privacy.android.domain.extension.MarkdownFileExtensionTest"`
Expected: PASS (all 4 tests).

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/mega/privacy/android/domain/extension/MarkdownFileExtension.kt domain/src/test/kotlin/mega/privacy/android/domain/extension/MarkdownFileExtensionTest.kt
git commit -m "AND-24001 Add isMarkdownFile detection helper"
```

---

## Task 3: Add the `TextEditorMarkdownRendering` feature flag

**Files:**
- Modify: `domain/src/main/kotlin/mega/privacy/android/domain/featuretoggle/ApiFeatures.kt:285-290`

- [ ] **Step 1: Add the enum entry**

In `ApiFeatures.kt`, add a new entry before the final `SyncUseCloudExplorerPicker` entry (keep the `;` on the last entry only). Insert:

```kotlin
    /**
     * Render Markdown (.md/.markdown) files in the Compose text editor (AND-24001).
     * When enabled, Markdown files default to a formatted read-only Preview with a
     * Preview/Source toggle. When disabled, Markdown files behave as plain text.
     */
    TextEditorMarkdownRendering(
        experimentName = "temd",
        description = "Render Markdown files in the text editor with a preview/source toggle",
        singleCheckPerRun = true,
        defaultValue = false,
    ),
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :domain:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add domain/src/main/kotlin/mega/privacy/android/domain/featuretoggle/ApiFeatures.kt
git commit -m "AND-24001 Add TextEditorMarkdownRendering feature flag"
```

---

## Task 4: `TextEditorContentView` enum

**Files:**
- Create: `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorContentView.kt`

- [ ] **Step 1: Create the enum**

```kotlin
package mega.privacy.android.feature.texteditor.presentation.model

/**
 * Which view a Markdown file is shown in (View mode only).
 * - [Preview]: formatted, rendered Markdown.
 * - [Source]: raw text in the existing chunked editor view.
 */
enum class TextEditorContentView {
    Preview,
    Source,
}
```

- [ ] **Step 2: Commit**

```bash
git add feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorContentView.kt
git commit -m "AND-24001 Add TextEditorContentView enum"
```

---

## Task 5: Extend `TextEditorComposeUiState`

**Files:**
- Modify: `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorComposeUiState.kt`

- [ ] **Step 1: Add fields + derived `isMarkdown`**

Add the import:

```kotlin
import mega.privacy.android.domain.extension.isMarkdownFile
```

Add two parameters to the data class (after `restoreFocusChunkIndex`):

```kotlin
    /** True when the Markdown-rendering feature flag is enabled for this session. */
    val isMarkdownEnabled: Boolean = false,
    /** Preview vs Source for a Markdown file (View mode only). */
    val contentView: TextEditorContentView = TextEditorContentView.Preview,
```

Add a derived property in the class body (add a body `{ }` to the data class):

```kotlin
) {
    /**
     * True when the current file should be treated as Markdown: the flag is on AND the
     * file name has a Markdown extension. Derived so it tracks rename/chat/link updates.
     */
    val isMarkdown: Boolean get() = isMarkdownEnabled && fileName.isMarkdownFile()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :feature:text-editor:text-editor:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorComposeUiState.kt
git commit -m "AND-24001 Add markdown fields to TextEditor UI state"
```

---

## Task 6: ViewModel — flag resolve, toggle, size-guarded full content (TDD)

**Files:**
- Modify: `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorComposeViewModel.kt`
- Test: `feature/text-editor/text-editor/src/test/java/mega/privacy/android/feature/texteditor/presentation/TextEditorComposeViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `TextEditorComposeViewModelTest.kt` (the `initUnderTest` helper, `getFeatureFlagValueUseCase` mock, and `nodeHandle` already exist). These stub the markdown flag explicitly per-test:

```kotlin
    @Test
    fun `test that isMarkdownEnabled is true when flag enabled and file is md`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(true)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        val state = underTest.uiState.value
        assertThat(state.isMarkdownEnabled).isTrue()
        assertThat(state.isMarkdown).isTrue()
        assertThat(state.contentView).isEqualTo(TextEditorContentView.Preview)
    }

    @Test
    fun `test that isMarkdown is false when flag disabled`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(false)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        assertThat(underTest.uiState.value.isMarkdown).isFalse()
    }

    @Test
    fun `test that getMarkdownPreviewContent returns null when a single line is too long`() = runTest {
        // One line longer than CHUNK_MAX_CHARS (50_000) — the AND-23707 long-line ANR case.
        val longLine = "x".repeat(CHUNK_MAX_CHARS + 1)
        doReturn(flowOf(listOf(longLine))).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(true)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        assertThat(underTest.getMarkdownPreviewContent()).isNull()
    }

    @Test
    fun `test that getMarkdownPreviewContent returns content for normal lines`() = runTest {
        doReturn(flowOf(listOf("# Title", "body"))).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(true)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        assertThat(underTest.getMarkdownPreviewContent()).isEqualTo("# Title\nbody")
    }

    @Test
    fun `test that toggleContentView flips Preview and Source`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(nodeHandle = any(), localPath = anyOrNull(), chunkSizeLines = any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        whenever(getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering))
            .thenReturn(true)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, fileName = "README.md")
        advanceUntilIdle()
        assertThat(underTest.uiState.value.contentView).isEqualTo(TextEditorContentView.Preview)
        underTest.toggleContentView()
        assertThat(underTest.uiState.value.contentView).isEqualTo(TextEditorContentView.Source)
        underTest.toggleContentView()
        assertThat(underTest.uiState.value.contentView).isEqualTo(TextEditorContentView.Preview)
    }
```

Add the missing imports to the test file:

```kotlin
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorContentView
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :feature:text-editor:text-editor:testDebugUnitTest --tests "*TextEditorComposeViewModelTest*"`
Expected: FAIL — unresolved `toggleContentView` / `isMarkdownEnabled` not set.

- [ ] **Step 3: Resolve the flag in init**

In `TextEditorComposeViewModel.kt`, inside the `loadJob` launch, right after the existing `longLineChunkingEnabled = ...` block (around line 221-223), add:

```kotlin
                val markdownRenderingEnabled = runCatching {
                    getFeatureFlagValueUseCase(ApiFeatures.TextEditorMarkdownRendering)
                }.getOrDefault(false)
                _uiState.update { it.copy(isMarkdownEnabled = markdownRenderingEnabled) }
```

- [ ] **Step 4: Add `toggleContentView()` and `getMarkdownPreviewContent()`**

Add a constant near the other chunk constants (top of file, after `CHUNK_SIZE_LINES`):

```kotlin
/** Upper bound on total Markdown preview size; above this the preview falls back to the raw
 *  chunked view to avoid ANR/OOM rendering one large Compose tree. Starting value — tune
 *  with the perf check (see techspec §4.4). The per-line guard reuses [CHUNK_MAX_CHARS]. */
internal const val MARKDOWN_PREVIEW_MAX_CHARS = 200_000
```

Add these functions to the class (e.g. near `getChunkText`):

```kotlin
    /** Flips the Markdown content view between Preview and Source (View mode only). */
    fun toggleContentView() {
        _uiState.update {
            it.copy(
                contentView = if (it.contentView == TextEditorContentView.Preview) {
                    TextEditorContentView.Source
                } else {
                    TextEditorContentView.Preview
                }
            )
        }
    }

    /**
     * Full document text for the Markdown preview, or null when the caller should fall back to
     * the raw chunked view. Two independent guards (see techspec §4.4):
     *  - total length must be ≤ [MARKDOWN_PREVIEW_MAX_CHARS], and
     *  - the longest single line must be ≤ [CHUNK_MAX_CHARS] — a single very long line
     *    (minified JSON, base64) ANRs in native text measurement (AND-23707); the un-chunked
     *    preview can't split it, so it must refuse. The chunked Source view handles it safely.
     */
    fun getMarkdownPreviewContent(): String? {
        val longestLine = fullContentLines.maxOfOrNull { it.length } ?: 0
        if (longestLine > CHUNK_MAX_CHARS) return null
        val content = fullContentLines.joinToString("\n")
        return content.takeIf { it.length <= MARKDOWN_PREVIEW_MAX_CHARS }
    }
```

Add the import:

```kotlin
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorContentView
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :feature:text-editor:text-editor:testDebugUnitTest --tests "*TextEditorComposeViewModelTest*"`
Expected: PASS (the three new tests + existing tests still green).

- [ ] **Step 6: Commit**

```bash
git add feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorComposeViewModel.kt feature/text-editor/text-editor/src/test/java/mega/privacy/android/feature/texteditor/presentation/TextEditorComposeViewModelTest.kt
git commit -m "AND-24001 Resolve markdown flag, add content-view toggle and size-guarded full content"
```

---

## Task 7: `MarkdownPreview` composable (snowflake-components)

**Files:**
- Create: `feature/text-editor/text-editor-snowflake-components/src/main/java/mega/privacy/android/feature/texteditor/components/MarkdownPreview.kt`

- [ ] **Step 1: Minimal render with library defaults (discover the API)**

Create the file with the renderer's default theming first — this compiles against the actual library API before we layer DSTokens on:

```kotlin
package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown

/**
 * Read-only Markdown preview. Renders the whole document in a single scroll container
 * (NOT the chunked LazyColumn — Markdown blocks span chunk boundaries). Use only for
 * content already size-checked by the ViewModel.
 */
@Composable
fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Markdown(
        content = content,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    )
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :feature:text-editor:text-editor-snowflake-components:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `com.mikepenz.markdown.m3.Markdown` is unresolved, open the resolved AAR / Maven page for `0.39.2` and correct the package (the m3 entry point lives in the `-m3` artifact).

- [ ] **Step 3: Apply DSTokens theming**

Replace the `Markdown(...)` call to pass MEGA colors/typography. The m3 artifact exposes `markdownColor()` and `markdownTypography()` default builders backed by `MaterialTheme`; override the text color with the design token and base typography on the core-ui styles:

```kotlin
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.tokens.theme.DSTokens
```

```kotlin
    Markdown(
        content = content,
        colors = markdownColor(
            text = DSTokens.colors.text.primary,
            linkText = DSTokens.colors.text.accent,
            codeText = DSTokens.colors.text.primary,
            dividerColor = DSTokens.colors.border.subtle,
        ),
        typography = markdownTypography(
            text = AppTheme.typography.bodyMedium,
            h1 = AppTheme.typography.headlineSmall,
            h2 = AppTheme.typography.titleLarge,
            h3 = AppTheme.typography.titleMedium,
            code = AppTheme.typography.bodyMedium,
        ),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    )
```

> The exact `markdownColor()` / `markdownTypography()` parameter names and the core-ui typography accessor (`AppTheme.typography` vs `DSTokens.typography`) must match the resolved versions — confirm against the lib's `0.39.2` API and the core-ui sources.jar. Keep only the parameters that exist; drop any that don't rather than inventing names. The compile step below catches mismatches.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :feature:text-editor:text-editor-snowflake-components:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add feature/text-editor/text-editor-snowflake-components/src/main/java/mega/privacy/android/feature/texteditor/components/MarkdownPreview.kt
git commit -m "AND-24001 Add MarkdownPreview composable with DSTokens theming"
```

---

## Task 8: Toggle action + view-mode top bar wiring

**Files:**
- Modify: `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorTopBarAction.kt`
- Modify: `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorScreen.kt`
- Modify: string resources (`strings_shared.xml`)

- [ ] **Step 1: Add the toggle string**

In the shared strings file (`shared/resources/.../res/values/strings_shared.xml`, alongside `text_editor_show_line_numbers`), add:

```xml
<string name="text_editor_toggle_markdown_view">Preview / source</string>
```

- [ ] **Step 2: Add the toggle action object**

In `TextEditorTopBarAction.kt`, add a new `data object` inside the sealed interface (mirror the `LineNumbers` object):

```kotlin
    data object ToggleMarkdownView : TextEditorTopBarAction {
        override val testTag: String = "text_editor_top_bar:toggle_markdown_view"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedR.string.text_editor_toggle_markdown_view)

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye)
    }
```

> Confirm `IconPack.Medium.Thin.Outline.Eye` exists in the generated IconPack; if not, pick the closest existing glyph (e.g. `Code`, `FileText`, `EyeOff`). The compile step catches an invalid name.

- [ ] **Step 3: Show the action + handle it in the view-mode top bar**

In `TextEditorScreen.kt`, `TextEditorViewModeTopAppBar` (around line 764) takes the actions list and `onMenuAction`. The composable needs to know whether the file is markdown — add a parameter and prepend the toggle. Change the signature to add `showMarkdownToggle: Boolean`, and update `buildList`:

```kotlin
    val actions = buildList {
        if (showMarkdownToggle) add(TextEditorTopBarAction.ToggleMarkdownView)
        add(TextEditorTopBarAction.LineNumbers)
        if (onOpenNodeOptions != null) add(TextEditorTopBarAction.More)
    }
```

At the `TextEditorViewModeTopAppBar(...)` call site (find where it's invoked in `TextEditorScreen`), pass `showMarkdownToggle = uiState.isMarkdown`. In the existing `onMenuAction` handler that the call site wires to the ViewModel, route the new action:

```kotlin
                is TextEditorTopBarAction.ToggleMarkdownView -> viewModel.toggleContentView()
```

(Add this branch wherever `onMenuAction`/`onActionPressed` maps top-bar actions to the ViewModel — next to the existing `LineNumbers` handling.)

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :feature:text-editor:text-editor:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/model/TextEditorTopBarAction.kt feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorScreen.kt shared/resources
git commit -m "AND-24001 Add Preview/Source toggle action to text editor top bar"
```

---

## Task 9: Screen — choose render path (Preview vs chunked)

**Files:**
- Modify: `feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorScreen.kt:359-403`

- [ ] **Step 1: Compute the preview content (size-guarded, remembered)**

In `TextEditorScreen`, near where `chunkCount`/providers are derived (before the content `when` block, ~line 158-200), add:

```kotlin
    val showMarkdownPreview = uiState.isMarkdown &&
        uiState.contentView == TextEditorContentView.Preview &&
        !isEditable
    val markdownContent = remember(showMarkdownPreview, uiState.contentVersion) {
        if (showMarkdownPreview) viewModel.getMarkdownPreviewContent() else null
    }
```

Add imports:

```kotlin
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorContentView
import mega.privacy.android.feature.texteditor.components.MarkdownPreview
```

- [ ] **Step 2: Branch the render path**

In the `else -> { Box { ... } }` content branch (line 359-403), render `MarkdownPreview` when `markdownContent` is non-null, otherwise the existing `TextEditorContent` + fast scrollbar. Wrap:

```kotlin
                else -> {
                    val preview = markdownContent
                    if (preview != null) {
                        MarkdownPreview(
                            content = preview,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            TextEditorContent(
                                // ... existing arguments unchanged ...
                            )
                            TextEditorFastScrollbar(
                                // ... existing arguments unchanged ...
                            )
                        }
                    }
                }
```

(Leave the `TextEditorContent`/`TextEditorFastScrollbar` argument lists exactly as they are today — only the surrounding `if (preview != null)` is new. When the size guard returns null, this falls through to the raw chunked view, satisfying the large-file fallback.)

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :feature:text-editor:text-editor:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the module unit tests**

Run: `./gradlew :feature:text-editor:text-editor:testDebugUnitTest`
Expected: PASS (no regressions).

- [ ] **Step 5: Commit**

```bash
git add feature/text-editor/text-editor/src/main/java/mega/privacy/android/feature/texteditor/presentation/TextEditorScreen.kt
git commit -m "AND-24001 Render Markdown preview path with size-guard fallback"
```

---

## Task 10: Build, device verification, and strings handoff

**Files:** none (verification + process).

- [ ] **Step 1: Assemble and install a debug build**

Run: `./gradlew installGmsDebug`
(Use a **debug** build, not QA — QA's runtime-override datastore short-circuits the real flag path. See techspec §4.6.)

- [ ] **Step 2: Enable the flag**

In the debug build's feature-flag dev settings, enable **TextEditorMarkdownRendering** (`temd`).

- [ ] **Step 3: Manual checklist (from techspec §7)**

Open a `.md` file from Cloud Drive and verify:
- Defaults to a formatted Preview (headings, bold/italic, ordered & unordered lists, links, inline code, code fences, blockquotes, tables).
- Top-bar toggle flips Preview ⇄ Source; Source shows the raw chunked view with line numbers/fast scroll.
- Preview → Edit → save returns to Preview; edit shows raw source.
- Tap a link → opens browser; non-`http(s)` links are no-ops.
- A `.md` with a remote image (`![](http://…)`) loads no image / makes no network call.
- A very large `.md` (> ~200k chars total) shows the raw view without ANR.
- A `.md` containing one very long line (> 50k chars on a single line, e.g. minified JSON — the AND-23707 case) shows the raw view without ANR.
- Light/dark theming looks correct; rotation keeps the Preview/Source choice; a fresh open defaults to Preview.
- With the flag **off**, a `.md` opens exactly as plain text (no toggle, no preview).
- A non-`.md` text file is unaffected with the flag on or off.

- [ ] **Step 4: Strings → Weblate**

The new string `text_editor_toggle_markdown_view` must go to Weblate before merge. Run the `/weblate` skill (from this worktree — note the worktree breaks branch detection; follow the manual checkout-dance workaround).

- [ ] **Step 5: Final full verification + review**

```bash
./gradlew :feature:text-editor:text-editor:testDebugUnitTest :feature:text-editor:text-editor-snowflake-components:testDebugUnitTest :domain:test --tests "*MarkdownFileExtensionTest*"
```
Then run `/android-code-review` on the branch before opening the MR.

---

## Self-Review

**Spec coverage:**
- Detect `.md`/`.markdown` → Task 2 (helper) + Task 6 (derived `isMarkdown`). ✅
- Read-only formatted preview → Task 7 (`MarkdownPreview`) + Task 9 (render path). ✅
- Preview ⇄ Source toggle → Task 4 (enum) + Task 6 (`toggleContentView`) + Task 8 (action/wiring). ✅
- Large-file size guard → Task 6 (`getMarkdownPreviewContent`: total `MARKDOWN_PREVIEW_MAX_CHARS` **and** per-line `CHUNK_MAX_CHARS` long-line guard, AND-23707) + Task 9 (null → raw). ✅
- DSTokens theming → Task 7 Step 3. ✅
- Feature flag → Task 3 + Task 6 (resolve) + flag-off behaviour in Task 10 checklist. ✅
- Remote images disabled → Task 1 omits the coil3 artifact (no image loader wired); Task 10 verifies no network call. ✅
- Edit stays raw / Preview→Edit→Preview → derived `isMarkdown` is false when `isEditable`; `showMarkdownPreview` requires `!isEditable` (Task 9); verified Task 10. ✅
- Independent scroll / read-through on Source only → `MarkdownPreview` owns its own `rememberScrollState`; `restoreScroll*` only feeds `TextEditorContent` (unchanged). ✅
- Persistence (survive rotation, reset on fresh open) → `contentView` lives in `_uiState` (survives recomposition/config change via the retained ViewModel); a fresh open constructs a new ViewModel defaulting to `Preview`. ✅

**Placeholder scan:** No `TBD`/`TODO`. Two explicitly-flagged confirm-at-compile spots (markdown lib param names in Task 7; IconPack glyph in Task 8) each have a guarding compile step — these are genuine third-party-API confirmations, not deferred work.

**Type consistency:** `isMarkdownFile()`, `TextEditorContentView.{Preview,Source}`, `isMarkdownEnabled`, `isMarkdown`, `contentView`, `toggleContentView()`, `getMarkdownPreviewContent()`, `MARKDOWN_PREVIEW_MAX_CHARS`, `ToggleMarkdownView` are used consistently across tasks.
