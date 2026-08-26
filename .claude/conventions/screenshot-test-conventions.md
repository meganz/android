# Screenshot Test Conventions

Compose screenshot tests live in `src/screenshotTest/kotlin/` and their reference images in
`src/screenshotTestDebug/reference/`. A test is a `@PreviewTest @CombinedThemePreviews @Composable`
function; each one produces two goldens, light and dark.

```kotlin
class SyncCardScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncCardPausedReasons() {
        AndroidThemeForPreviews {
            SyncCard(/* … */)
        }
    }
}
```

Use `AndroidThemeForPreviews`, never `OriginalTheme(isDark = false)` — a hardcoded flag renders the
"dark" golden on a light background, which makes the dark image worthless.

## Gradle tasks

| Task | Purpose |
|---|---|
| `:module:updateDebugScreenshotTest` | Record/overwrite goldens |
| `:module:validateDebugScreenshotTest` | Compare current render against goldens |

Always pass `--rerun`. Gradle reports `UP-TO-DATE` for a task that did not execute, and that reads
as a pass. Confirm the task line appears in the output without `UP-TO-DATE` before believing a green
result.

The full record → test → validate chain can exceed a ten-minute command timeout; run the tasks
separately.

## Changing existing UI

The point of a golden is the comparison, not the green tick. A migration or restyle **should** fail
validation — that failure is the artifact you review.

1. **Record the baseline against the current implementation, first.** A baseline recorded after the
   change proves nothing.
2. Make the change.
3. Run `validateDebugScreenshotTest --rerun` and expect it to fail.
4. **Open the diff, not the new render.** The plugin writes a highlighted comparison to
   `build/outputs/screenshotTest-results/preview/debug/diffs/…`. The sibling `rendered/…` directory
   holds only the new image; judging that on its own asks "does this look plausible?" when the
   question is "what changed?".
5. Inspect the **component-level** test, not just the screen-level one. A card inside a full-screen
   golden is a small element; in its own test it fills the frame.
6. State the visual deltas out loud before re-recording, so accepting them is a decision.
7. Re-record and commit the goldens with the change.

## Covering the states that matter

Coverage is per *state*, not per component. A component with a golden can still have no coverage of
the part you are about to change.

Before changing a component, check that a golden exists for the states its change affects:

- conditional slots — an error message, an empty state, a banner
- internal state a preview cannot reach. If a flag is `rememberSaveable` inside the composable, no
  preview can render the other value; hoist it first, then add the golden.
- strings that are *built* rather than used as-is — placeholder substitution, spans
- branches that select different resources — a button label chosen by a `when`
- landscape, where a layout narrows or reflows

## Goldens are environment-sensitive

Reference images depend on the rendering environment. `updateDebugScreenshotTest` re-records
**every** golden in the module, and unrelated ones will move on a machine whose fonts or density
differ from the one that recorded them.

Stage goldens by path. Never `git add -A` after recording. If images you did not touch appear
modified, revert them:

```bash
git status --porcelain -- <module>/src/screenshotTestDebug
git checkout -- <module>/src/screenshotTestDebug   # discard environment drift
```

## Test-only visibility

Screenshot tests share the module, so `internal` is enough — a `private` composable can be widened
to `internal` to make it testable. Prefer that over restructuring production code for a test.

Screens driven by view models generally cannot be goldened as a whole. Pull the stateless part out
and test that: a chip row, a card, a dialog body. `SyncListScreen` is view-model driven, but its
`HeaderChips` is not, and its spacing regressed unnoticed precisely because no golden covered it.
