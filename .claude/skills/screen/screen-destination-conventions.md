# Screen Destination Conventions

## File Structure

Each destination file contains:
1. The `@Serializable` NavKey (if feature-local)
2. The `EntryProviderScope<NavKey>` extension function

```kotlin
@Serializable
data object <Name> : NavKey

fun EntryProviderScope<NavKey>.<name>Screen(
    navigationHandler: NavigationHandler,
) {
    entry<<Name>> {
        val viewmodel = hiltViewModel<<Name>ViewModel>()
        val state by viewmodel.uiState.collectAsStateWithLifecycle()
        <Name>Screen(
            state = state,
            onAction = viewmodel::handleAction,
        )
    }
}
```

## NavKey Conventions

### Simple NavKey (no parameters)

```kotlin
@Serializable
data object <Name> : NavKey
```

Use when the screen requires no arguments from the caller.

### Parameterized NavKey

```kotlin
@Serializable
data class <Name>NavKey(
    val id: Long,
    val name: String? = null,
) : NavKey
```

- All properties must be `@Serializable`-compatible types (primitives, `String`, `enum`, or `@Serializable` classes).
- Provide sensible defaults where appropriate so callers can omit optional parameters.
- For parameterized keys, suffix the class name with `NavKey` (e.g., `CloudDriveNavKey`, `RecentsBucketScreenNavKey`) to distinguish from the screen name.

### NavKey Marker Interfaces

Choose the appropriate marker based on the screen's role:

| Marker | When to use |
|--------|-------------|
| `NavKey` | Default — standard screen |
| `MainNavItemNavKey` | Bottom navigation item (home, cloud drive, etc.) |
| `NoNodeNavKey` | Screen that does not require a valid node context |
| `NoSessionNavKey.Optional` | Screen accessible without a logged-in session (but session is optional) |
| `NoSessionNavKey.Mandatory` | Screen that must not have a session (login, onboarding) |
| `DialogNavKey` | Dialog or bottom sheet destination |

### NavKey Placement

- **Feature-local** (default): define at the top of the destination file. Use when only the feature module references this NavKey.
- **Shared**: define in `navigation/src/main/java/mega/privacy/android/navigation/destination/`. Use when other feature modules or the app module need to `navigationHandler.navigate(<NavKey>)` to this screen.

## Extension Function Conventions

### Function Naming

camelCase version of the screen name, suffixed with `Screen`:
- `HomeConfigurationScreen` → `homeConfigurationScreen`
- `CloudDriveScreen` → `cloudDriveScreen`
- `ShareToMegaScreen` → `shareToMegaScreen`

### Function Signature

```kotlin
fun EntryProviderScope<NavKey>.<name>Screen(
    navigationHandler: NavigationHandler,
) {
```

- First parameter is always `navigationHandler: NavigationHandler`.
- Add `transferHandler: TransferHandler` only if the screen triggers transfer events.
- Add specific navigation lambdas (e.g., `onNavigateBack: (NavKey) -> Unit`) only when the destination needs navigation behavior not directly expressible via `navigationHandler` methods.

### Standard ViewModel Wiring

```kotlin
entry<<NavKeyType>> {
    val viewmodel = hiltViewModel<<Name>ViewModel>()
    val state by viewmodel.uiState.collectAsStateWithLifecycle()
    <Name>Screen(
        state = state,
        onAction = viewmodel::handleAction,
        onNavigateBack = navigationHandler::back,
    )
}
```

- Use `hiltViewModel<T>()` for standard Hilt injection.
- Collect state with `by viewmodel.uiState.collectAsStateWithLifecycle()` (match the ViewModel's actual property name — usually `uiState` or `state`).
- Wire ViewModel methods as method references: `viewmodel::methodName`.
- Wire navigation actions as method references: `navigationHandler::back`.

### Assisted Injection Wiring

When the ViewModel requires runtime parameters from the NavKey:

```kotlin
entry<<NavKeyType>> { key ->
    val viewmodel = hiltViewModel<<Name>ViewModel, <Name>ViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                <Name>ViewModel.Args(
                    id = key.id,
                    name = key.name,
                )
            )
        }
    )
    val state by viewmodel.uiState.collectAsStateWithLifecycle()
    <Name>Screen(
        state = state,
        onAction = viewmodel::handleAction,
    )
}
```

- Access NavKey via the `key` lambda parameter of `entry<T> { key -> }`.
- Map NavKey properties to ViewModel `Args` data class — **never pass the NavKey itself** to the ViewModel.
- The `Args` data class is defined inside the ViewModel (see viewmodel-conventions.md).

### Metadata

When the entry needs metadata (e.g., analytics screen view events):

```kotlin
entry<<NavKeyType>>(
    metadata = buildMetadata {
        withScreenViewEvent(<ScreenEvent>)
    }
) {
    // ...
}
```

- Import `buildMetadata` from `mega.privacy.android.navigation.contract.metadata`.
- Import `withScreenViewEvent` from the analytics module.

### Navigation Callback Wiring

Wire navigation actions as method references on `navigationHandler`:

| Screen callback | Destination wiring |
|---|---|
| `onNavigateBack: () -> Unit` | `navigationHandler::back` |
| `onNavigate: (NavKey) -> Unit` | `navigationHandler::navigate` |
| `onRemove: (NavKey) -> Unit` | `navigationHandler::remove` |

For navigation to specific destinations, create an inline lambda that wraps the NavKey construction:

```kotlin
onNavigateToDetail = { id ->
    navigationHandler.navigate(DetailNavKey(id = id))
}
```

This keeps the screen unaware of NavKey types — it only knows about domain-level parameters (e.g., `Long`, `String`, `NodeId`).

### ViewModel Method Reference Wiring

Wire ViewModel actions as method references:

```kotlin
onDeleteItem = viewmodel::deleteItem,
onToggleEnabled = viewmodel::updateEnabledState,
onEventConsumed = viewmodel::onNavigationEventConsumed,
```

## FeatureDestination Registration

### Adding to an Existing FeatureDestination

Add the destination call to the `navigationGraph` lambda:

```kotlin
class <Feature>FeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->
            <name>Screen(navigationHandler = navigationHandler)
            // other screens...
        }
}
```

### Creating a New FeatureDestination

When creating a new FeatureDestination class for a feature module:

```kotlin
class <Feature>FeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->
            <name>Screen(navigationHandler = navigationHandler)
        }
}
```

### DI Module Registration

Each FeatureDestination must be registered in a Hilt module with `@IntoSet` multibinding:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
class <Feature>NavigationModule {
    @Provides
    @IntoSet
    fun provide<Feature>FeatureDestination(): FeatureDestination = <Feature>FeatureGraph()
}
```

- Place the module in the feature's `di/` package.
- If the FeatureDestination has `@Inject constructor(deps...)`, inject it as a parameter instead of constructing directly.

## Required Imports

### Standard destination

```kotlin
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.NavigationHandler
```

### With TransferHandler

```kotlin
import mega.privacy.android.navigation.contract.TransferHandler
```

### With metadata

```kotlin
import mega.privacy.android.navigation.contract.metadata.buildMetadata
```

### With marker interfaces

```kotlin
// Choose the appropriate import:
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
```
