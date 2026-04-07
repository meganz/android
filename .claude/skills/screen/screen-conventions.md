# Screen Composable Conventions

## Function Signature

```kotlin
@Composable
fun <Name>Screen(
    state: <Name>UiState,
    onAction1: (Param) -> Unit,
    onAction2: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ...
}
```

## Key Rules

- **Stateless** — the screen composable receives all data via parameters. It holds no ViewModel reference and performs no data fetching.
- **State parameter** — always the first parameter. Type is the ViewModel's UI state class (sealed interface or data class).
- **Lambda callbacks** — all user actions and navigation triggers are expressed as lambda parameters. Named with `on` prefix (e.g., `onBackClick`, `onItemSelected`, `onNavigateToDetail`).
- **Never receive**:
    - `NavController` or `NavigationHandler`
    - NavKey classes or any navigation-layer types
    - ViewModel instances
    - `TransferHandler` or `TransferTriggerEvent`
- **Never import** navigation key classes or navigation contract types.
- **Modifier parameter** — always include `modifier: Modifier = Modifier` as the last parameter (after state and callbacks).
- **Visibility** — match the existing pattern in the target module. Most current screens in the codebase are public, but `internal` is preferred for feature-scoped screens when possible.

## Layout Structure

Use `MegaScaffold` as the top-level layout:

```kotlin
@Composable
fun <Name>Screen(
    state: <Name>UiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { /* optional top bar */ },
    ) { paddingValues ->
        when (state) {
            <Name>UiState.Loading -> {
                // Loading indicator or empty
            }
            is <Name>UiState.Data -> {
                <Name>Content(
                    modifier = Modifier.padding(paddingValues),
                    state = state,
                    onBackClick = onBackClick,
                )
            }
        }
    }
}
```

## Content Extraction

For non-trivial screens, extract the main content into a separate composable in the same file:

```kotlin
@Composable
private fun <Name>Content(
    state: <Name>UiState.Data,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Content implementation
}
```

- The content composable receives the `Data` substate directly (not the sealed parent).
- Keep it `private` — it is an implementation detail of the screen.
- Pass through only the callbacks it needs.

## State Event Handling

When the ViewModel exposes one-shot events via `StateEventWithContent`, handle them in the screen using `EventEffect`:

```kotlin
EventEffect(
    event = state.navigationEvent,
    onConsumed = onNavigationEventConsumed,
) { destination ->
    onNavigateToDetail(destination)
}
```

- `EventEffect` must be placed inside the `Data` branch of the `when` block (since events live on the `Data` substate).
- The `onConsumed` callback is wired to the ViewModel's consume method in the destination.

## Test Tags

Add test tags to key composables for UI testing:

```kotlin
const val TEST_TAG_<NAME>_SCREEN = "<feature>:screen"
const val TEST_TAG_<NAME>_ITEM = "<feature>:item_"
```

- Define constants at the bottom of the screen file.
- Use `Modifier.testTag(...)` on key composables.

## Preview

Include a `@Preview` composable for the screen when practical:

```kotlin
@Preview
@Composable
private fun <Name>ScreenPreview() {
    <Name>Screen(
        state = <Name>UiState.Data(
            // preview data
        ),
        onAction1 = {},
        onAction2 = {},
    )
}
```

## File Placement

```
<module>/src/main/java/<package>/
    <Name>Screen.kt
    <Name>ScreenDestination.kt
    model/
        <Name>UiState.kt
```

The Screen and Destination files live side by side in the same package (typically under `presentation/<feature>/`).

## Required Imports

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.MegaScaffold
```
