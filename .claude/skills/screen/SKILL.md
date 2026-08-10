---
name: screen
description: >
  Create, update, and refactor Android Compose screens with Navigation3 destinations.
  Creates stateless Screen composables, Destination files (NavKey + EntryProviderScope extension),
  and registers destinations in FeatureDestination classes. Supports simple and parameterized
  NavKeys, navigation callbacks, metadata, and assisted ViewModel injection. Does NOT create
  ViewModels (use /viewmodel for that).
triggers:
  - /screen
  - create screen
  - new screen
  - update screen
  - refactor screen
---

# Screen + Destination Skill

Create, update, and refactor Android Compose screens with Navigation3 destinations.

## Usage

```
/screen create HomeConfiguration --module feature/home/home
/screen create HomeConfiguration --module feature/home/home --navkey-type object
/screen create CloudDrive --module feature/cloud-drive/cloud-drive --navkey-type class --assisted
/screen create Home --module feature/home/home --navkey-marker MainNavItemNavKey --metadata
/screen update HomeConfiguration --module feature/home/home --change "add onBackClick callback"
/screen refactor CloudDrive --module feature/cloud-drive/cloud-drive
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `create\|update\|refactor` | Mode of operation (required, first positional arg) | `create` |
| Screen name | Name of the screen (required, second positional arg). Used to derive Screen, Destination, and NavKey names. | `HomeConfiguration` |
| `--module <path>` | Module path relative to project root (required) | `--module feature/home/home` |
| `--navkey-type <type>` | NavKey form: `object` (default, no params) or `class` (with params) | `--navkey-type class` |
| `--navkey-marker <interface>` | NavKey marker interface: `NavKey` (default), `MainNavItemNavKey`, `NoNodeNavKey`, `NoSessionNavKey`, `DialogNavKey` | `--navkey-marker MainNavItemNavKey` |
| `--assisted` | ViewModel uses assisted injection (Factory pattern) | `--assisted` |
| `--metadata` | Include `buildMetadata { }` block in entry | `--metadata` |
| `--change <desc>` | Description of what to change (for `update` mode) | `--change "add onBackClick callback"` |

---

## Execution Steps

### Mode: CREATE

#### Step 1 — Gather Requirements

1. Confirm the screen name, module path, and feature description with the user.
2. Search the target module for existing screens, destinations, and FeatureDestination classes to match local package structure and conventions:
   ```
   Glob: <module>/src/main/**/presentation/**/*Screen.kt
   Glob: <module>/src/main/**/presentation/**/*Destination.kt
   Glob: <module>/src/main/**/navigation/**/*FeatureDestination.kt
   Glob: <module>/src/main/**/navigation/**/*FeatureGraph.kt
   ```
3. Determine the package name by following existing module conventions.
4. Check if a ViewModel already exists for this screen:
   ```
   Glob: <module>/src/main/**/presentation/**/<Name>ViewModel.kt
   ```
   If not found, inform the user they should use `/viewmodel create` to create one before or after this skill completes.
5. If the ViewModel exists, read it to identify:
   - The state flow property name (usually `uiState` or `state`)
   - The UI state type name
   - Public methods that should be wired as callbacks
6. Ask the user:
   - What user action callbacks does this screen need? (e.g., `onBackClick`, `onItemClick`, `onDeleteItem`)
   - Does this screen need navigation callbacks? (e.g., `onNavigateToDetail`, `onNavigateBack`) — these will be wired to `navigationHandler` method references in the destination.
   - Does the NavKey need parameters? (If yes, `--navkey-type class` is implied.) What are the parameters and their types?
   - Does the ViewModel use assisted injection? (If yes, `--assisted` is implied.)
   - Should the NavKey be placed locally in the destination file, or in the shared `navigation/src/main/.../destination/` module? (Local is the default for feature-scoped screens; shared is required when other modules need to navigate to this screen.)
   - Does the entry need metadata? (e.g., analytics screen view events via `withScreenViewEvent`)
   - Does this feature module already have a FeatureDestination/FeatureGraph class? (The glob from step 2 will tell you.)

#### Step 2 — Propose File Plan (User Confirmation Gate)

Based on the gathered requirements, propose the files to be created/modified. Present to the user:

```
Proposed files for <Name>Screen:

CREATE:
1. `<module>/src/main/java/<package>/<Name>Screen.kt`
   - Stateless @Composable function
   - Parameters: state: <Name>UiState, <callback lambdas>

2. `<module>/src/main/java/<package>/<Name>ScreenDestination.kt`
   - @Serializable NavKey: data object <Name> : NavKey
   - Extension function: EntryProviderScope<NavKey>.<name>Screen(navigationHandler, ...)
   - Wires ViewModel state + method references to Screen composable

MODIFY (if FeatureDestination exists):
3. `<module>/src/main/java/<package>/navigation/<Feature>FeatureGraph.kt`
   - Register <name>Screen(...) call in navigationGraph

CREATE (if no FeatureDestination exists):
3a. `<module>/src/main/java/<package>/navigation/<Feature>FeatureGraph.kt`
3b. `<module>/src/main/java/<package>/di/<Feature>NavigationModule.kt`

Please confirm, modify, or add to this plan before proceeding.
```

**Do NOT proceed until the user confirms the file plan.**

#### Step 3 — Generate Screen Composable

Create the screen file at `<module>/src/main/java/<package>/<Name>Screen.kt` following the Screen Standards below.

Key rules enforced during generation:
- The screen composable is **stateless** — it receives `state` and lambda callbacks only.
- **Never** pass `NavController`, `NavigationHandler`, or any NavKey type as a parameter.
- **Never** import or reference navigation keys in the screen file.
- Navigation actions are expressed as lambda callbacks (e.g., `onNavigateBack: () -> Unit`).
- Use `MegaScaffold` for the top-level layout.
- Handle `Loading` and `Data` substates via `when` if using sealed state.

#### Step 4 — Generate Destination File

Create the destination file at `<module>/src/main/java/<package>/<Name>ScreenDestination.kt` following the Destination Standards below.

Key rules enforced during generation:
- NavKey is `@Serializable` and defined at the top of the file (for feature-local keys) or imported from `navigation/destination/` (for shared keys).
- The extension function receives `NavigationHandler` (and optionally `TransferHandler` or other dependencies).
- ViewModel is created via `hiltViewModel<T>()` (standard) or `hiltViewModel<T, T.Factory> { it.create(...) }` (assisted).
- State is collected via `by viewmodel.uiState.collectAsStateWithLifecycle()` (or `viewmodel.state` — match the actual ViewModel property name).
- Navigation callbacks are wired as method references: `onNavigateBack = navigationHandler::back`, `onNavigate = { navigationHandler.navigate(TargetNavKey(it)) }`.
- ViewModel action callbacks are wired as method references: `onDeleteItem = viewmodel::deleteItem`.

#### Step 5 — Register in FeatureDestination

If the feature module already has a FeatureDestination/FeatureGraph class:
- Add the `<name>Screen(navigationHandler = navigationHandler)` call to the `navigationGraph` lambda.

If no FeatureDestination exists:
- Create `<Feature>FeatureGraph` class implementing `FeatureDestination`.
- Create or update the DI module with `@Provides @IntoSet` binding for `FeatureDestination`.

#### Step 6 — Verify Compilation

Run a compile check:
```bash
./gradlew <module-gradle-path>:compileDebugKotlin
```

If compilation fails, fix the issues and re-run.

---

### Mode: UPDATE

#### Step 1 — Locate Files

Find the existing screen, destination, and FeatureDestination:
```
Glob: **/<Name>Screen.kt
Glob: **/<Name>ScreenDestination.kt
Glob: **/<Name>Destination.kt
```

Read all located files to understand the current implementation.

#### Step 2 — Check for Anti-Patterns

Inspect the screen and destination for anti-patterns:
- Screen receives `NavController`, `NavigationHandler`, or NavKey types directly
- Screen imports navigation key classes
- Destination passes ViewModel directly to screen (instead of state + callbacks)
- Missing `@Serializable` on NavKey
- ViewModel created outside `entry { }` block

If found:
- **Warn the user** that the screen/destination uses a non-standard pattern.
- **Suggest running `/screen refactor`** first to align with conventions before adding new functionality.
- If the user wants to proceed without refactoring, continue but maintain consistency within the file.

#### Step 3 — Propose Changes (User Confirmation Gate)

Based on the `--change` description, propose the specific modifications. Present them to the user for confirmation.

**Do NOT proceed until the user confirms.**

#### Step 4 — Apply Changes

Modify the screen, destination, and/or FeatureDestination as needed. Ensure:
- New callbacks added to the screen are wired in the destination.
- New NavKey parameters are `@Serializable`-compatible types.
- Existing callback wiring is not broken.

#### Step 5 — Verify Compilation

Run compilation check:
```bash
./gradlew <module-gradle-path>:compileDebugKotlin
```

---

### Mode: REFACTOR

#### Step 1 — Analyze

Locate and read the screen, destination, FeatureDestination, and ViewModel. Identify:
- Screen receiving `NavController`, `NavigationHandler`, `ViewModel`, or NavKey types directly (should receive state + lambdas only)
- Screen importing navigation key classes
- Destination not using `collectAsStateWithLifecycle()`
- Missing `@Serializable` on NavKey
- Old Jetpack Navigation Compose patterns (`NavHost`, `composable()`, `NavGraphBuilder`) that should migrate to Navigation3 (`EntryProviderScope`, `entry<T>`)
- ViewModel created with `viewModel()` instead of `hiltViewModel()`
- Destination not using method references for ViewModel actions
- Navigation callbacks hardcoded in destination instead of being passed as parameters

Present the analysis to the user.

#### Step 2 — Plan Refactoring

Present a step-by-step refactoring plan to the user. Each step should be a small, verifiable change.

Common refactoring sequences:
1. Extract state collection from screen into destination
2. Replace ViewModel parameter with state + lambda callbacks
3. Remove NavKey/navigation imports from screen file
4. Wire navigation actions through lambda callbacks
5. Add `@Serializable` to NavKey
6. Migrate from `NavGraphBuilder.composable()` to `EntryProviderScope.entry<T>()`

#### Step 3 — Apply Incrementally

For each refactoring step:
1. Make the change
2. Run compilation check to verify nothing broke
3. If compilation fails, fix or revert before proceeding

---

## Screen Standards

See [screen-conventions.md](./screen-conventions.md) for all Screen composable conventions.

---

## Destination Standards

See [screen-destination-conventions.md](./screen-destination-conventions.md) for all Destination and NavKey conventions.

---

## Guidelines

1. **No TDD for screens** — unlike ViewModels, use cases, and mappers, screen composables and destinations are not created with TDD. Compilation verification is used instead.
2. **User confirmation gate** — always present the proposed file plan and wait for explicit user approval before writing any code.
3. **Module convention matching** — before generating code, search the target module for existing screens, destinations, and FeatureDestination classes. Match the local package structure, naming patterns, and any module-specific conventions.
4. **Stateless screen composable** — the screen composable must never receive a ViewModel, NavController, NavigationHandler, or NavKey. It receives only UI state and lambda callbacks. This is a hard rule with no exceptions.
5. **Destination is the wiring layer** — the destination file is responsible for creating the ViewModel, collecting state, and mapping ViewModel methods and navigation actions to screen callbacks.
6. **ViewModel is not created by this skill** — this skill assumes the ViewModel either already exists or will be created via `/viewmodel`. The destination wires them together, but this skill does not generate ViewModel code.
7. **NavKey placement** — if the NavKey is only used within the feature module, define it locally in the destination file. If other modules need to navigate to this screen, place the NavKey in `navigation/src/main/java/mega/privacy/android/navigation/destination/`.
8. **Minimal changes** — when updating or refactoring, change only what is necessary. Do not refactor unrelated code.
