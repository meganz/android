---
name: create-module
description: >
  Create new Gradle modules in the Android project. Supports feature (simple and nested with
  snowflakes), core, and shared module types. Creates all required files (build file,
  .gitignore, AndroidManifest.xml, source directories) and registers the module in settings.gradle.kts.
triggers:
  - /create-module
  - create module
  - new module
  - add module
  - create feature module
  - new feature module
  - create core module
  - create shared module
---

# Create Module Skill

Create new Gradle modules in the Android project with the correct file structure, build configuration, and settings registration.

## Usage

```
/create-module feature my-feature
/create-module feature my-feature --nested
/create-module core my-utility
/create-module shared my-shared-feature
/create-module feature my-feature --plugins room,parcelize
/create-module core my-utility --minimal
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `feature\|core\|shared` | Module category (required, first positional arg) | `feature` |
| Module name | Kebab-case name for the module (required, second positional arg) | `my-feature` |
| `--nested` | Create a nested structure with a snowflakes sub-module (feature only) | `--nested` |
| `--plugins <list>` | Comma-separated additional plugins: `room`, `parcelize`, `serialisation` | `--plugins room,parcelize` |
| `--no-compose` | Omit the Compose plugin (default includes Compose for feature and shared) | `--no-compose` |
| `--no-hilt` | Omit the Hilt plugin | `--no-hilt` |
| `--minimal` | Use only `mega.android.library` plugin (useful for core utility modules) | `--minimal` |

---

## Execution Steps

### Step 1 — Gather and Confirm Parameters

1. If the user did not provide both the category and module name, ask for the missing values.
2. Validate the module name is kebab-case (lowercase letters, numbers, and hyphens only).
3. Determine the module structure:
   - **Simple feature**: `feature/{name}/{name}.gradle.kts` — included as `:feature:{name}`
   - **Nested feature (main)**: `feature/{name}/{name}/{name}.gradle.kts` — included as `:feature:{name}:{name}`
   - **Nested feature (snowflake)**: `feature/{name}/{name}-snowflakes/{name}-snowflakes.gradle.kts` — included as `:feature:{name}:{name}-snowflakes`
   - **Core**: `core/{name}/{name}.gradle.kts` — included as `:core:{name}`
   - **Shared**: `shared/{name}/{name}.gradle.kts` — included as `:shared:{name}`
4. Determine the namespace (see Namespace Rules below).
5. Determine the plugin set (see Plugin Selection below).
6. Present a summary to the user for confirmation. Example:

```
Module creation plan:

Category: feature (nested)
Module name: my-feature

Directories to create:
  - feature/my-feature/my-feature/
  - feature/my-feature/my-feature-snowflakes/

Files to create:
  - feature/my-feature/my-feature/my-feature.gradle.kts
    namespace: mega.privacy.android.feature.myfeature
  - feature/my-feature/my-feature/.gitignore
  - feature/my-feature/my-feature/src/main/AndroidManifest.xml
  - feature/my-feature/my-feature/src/main/java/  (empty source dir)
  - feature/my-feature/my-feature/src/test/java/  (empty test dir)
  Navigation scaffolding (main module only):
  - navigation/.../destination/MyFeatureDestinations.kt  (NavKey)
  - .../presentation/MyFeatureScreenDestination.kt
  - .../navigation/MyFeatureFeatureGraph.kt
  - .../di/MyFeatureModule.kt
  - feature/my-feature/my-feature-snowflakes/my-feature-snowflakes.gradle.kts
    namespace: mega.privacy.android.feature.myfeature.components
  - feature/my-feature/my-feature-snowflakes/.gitignore
  - feature/my-feature/my-feature-snowflakes/src/main/AndroidManifest.xml
  - feature/my-feature/my-feature-snowflakes/src/main/java/  (empty source dir)
  - feature/my-feature/my-feature-snowflakes/src/test/java/  (empty test dir)

settings.gradle.kts entries:
  - include(":feature:my-feature:my-feature")
  - include(":feature:my-feature:my-feature-snowflakes")

Plugins (main): mega.android.library, mega.android.library.compose, mega.android.hilt, kotlin.serialisation, kotlin-android
Plugins (snowflake): mega.android.library, mega.android.library.compose, mega.android.hilt, kotlin.serialisation, kotlin-android

Proceed?
```

**Do NOT proceed until the user confirms.**

### Step 2 — Create Directory Structure

Create the required directories for each module being created:
```bash
mkdir -p <module-path>/src/main/java
mkdir -p <module-path>/src/test/java
```

For **feature modules** (simple or nested main, but NOT snowflakes), also create the navigation scaffolding packages:
```bash
mkdir -p <module-path>/src/main/java/<package-path>/presentation
mkdir -p <module-path>/src/main/java/<package-path>/navigation
mkdir -p <module-path>/src/main/java/<package-path>/di
```
Where `<package-path>` is the namespace with dots replaced by `/` (e.g., `mega/privacy/android/feature/myfeature`).

### Step 3 — Create Build File

Generate the build file at `<module-path>/<module-name>.gradle.kts` using the appropriate template from the Build File Templates section below.

Apply plugin modifications based on flags:
- `--plugins room` → add `alias(convention.plugins.mega.android.room)` after the hilt plugin line
- `--plugins parcelize` → add `id("kotlin-parcelize")` after the `id("kotlin-android")` line
- `--plugins serialisation` → add `alias(plugin.plugins.kotlin.serialisation)` (already present in feature/snowflake templates)
- `--minimal` → use only `alias(convention.plugins.mega.android.library)` regardless of category
- `--no-compose` → omit `alias(convention.plugins.mega.android.library.compose)`
- `--no-hilt` → omit `alias(convention.plugins.mega.android.hilt)`

### Step 4 — Create .gitignore

Create `<module-path>/.gitignore`:
```
/build
```

### Step 5 — Create AndroidManifest.xml

Create `<module-path>/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

</manifest>
```

### Step 6 — Create Navigation Scaffolding (Feature Modules Only)

For **feature modules** (simple or nested main, but NOT snowflakes or `--minimal`), create three scaffolding files. These wire the feature into the app's navigation system.

Use the following naming conventions (where `{Name}` is the PascalCase feature name, e.g., `my-feature` → `MyFeature`, and `{package}` is the namespace, e.g., `mega.privacy.android.feature.myfeature`):

#### 6a — NavKey: `navigation/src/main/java/mega/privacy/android/navigation/destination/{Name}Destinations.kt`

The NavKey is the shared entry point to the feature and lives in the **navigation module**, not the feature module. Create it at `navigation/src/main/java/mega/privacy/android/navigation/destination/{Name}Destinations.kt`:

```kotlin
package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object {Name}ScreenNavKey : NavKey
```

#### 6b — ScreenDestination: `<module-path>/src/main/java/<package-path>/presentation/{Name}ScreenDestination.kt`

This is a placeholder screen destination that the user will flesh out later. It provides a minimal composable entry point. It imports the NavKey from the navigation module.

```kotlin
package {package}.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.{Name}ScreenNavKey

fun EntryProviderScope<NavKey>.{camelName}Screen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<{Name}ScreenNavKey> {
        // TODO: Add screen composable
    }
}
```

Where `{camelName}` is the camelCase feature name (e.g., `my-feature` → `myFeature`).

#### 6c — FeatureGraph: `<module-path>/src/main/java/<package-path>/navigation/{Name}FeatureGraph.kt`

```kotlin
package {package}.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import {package}.presentation.{camelName}Screen

class {Name}FeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->
            {camelName}Screen(
                navigationHandler = navigationHandler,
                transferHandler = transferHandler,
            )
        }
}
```

#### 6d — Hilt Module: `<module-path>/src/main/java/<package-path>/di/{Name}Module.kt`

```kotlin
package {package}.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.navigation.contract.FeatureDestination
import {package}.navigation.{Name}FeatureGraph

@Module
@InstallIn(SingletonComponent::class)
class {Name}Module {

    @Provides
    @IntoSet
    fun provide{Name}FeatureDestination(): FeatureDestination = {Name}FeatureGraph()
}
```

---

### Step 7 — Register in settings.gradle.kts

Add the `include()` line(s) to `settings.gradle.kts`.

**Placement rules:**
- All `include()` lines must be placed BEFORE the `rootProject.children.forEach` block (before the comment `// Configure modules to use their own name as the build file name` around line 91).
- Group with same-category modules:
  - Feature modules: after the last existing `include(":feature:...")` line
  - Core modules: after the last existing `include(":core:...")` line
  - Shared modules: after the last existing `include(":shared:...")` line
- For nested feature modules, place the main module include line immediately before the snowflakes include line.

**Include path format:**
- Simple feature: `include(":feature:{name}")`
- Nested feature main: `include(":feature:{name}:{name}")`
- Nested feature snowflake: `include(":feature:{name}:{name}-snowflakes")`
- Core: `include(":core:{name}")`
- Shared: `include(":shared:{name}")`

### Step 8 — Verify

Run a Gradle dry-run to verify the module is recognized:
```bash
./gradlew :<gradle-path>:tasks --dry-run 2>&1 | head -5
```

Where `<gradle-path>` uses colons as separators (e.g., `feature:my-feature` or `feature:my-feature:my-feature`).

Report success or failure to the user.

If the module was created as nested with snowflakes, remind the user that the main module can depend on the snowflake module via:
```kotlin
implementation(project(":feature:{name}:{name}-snowflakes"))
```

---

## Build File Templates

### Feature Module (simple or nested main)

```kotlin
import mega.privacy.android.build.preBuiltSdkDependency

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    lint {
        abortOnError = true
    }
    namespace = "<namespace>"
}

dependencies {
    preBuiltSdkDependency(rootProject.extra)

    lintChecks(project(":lint"))

    implementation(project(":core:navigation-contract"))
    implementation(project(":domain"))
    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))

    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
```

### Snowflake-Components Module

```kotlin
plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    alias(plugin.plugins.kotlin.serialisation)
    id("kotlin-android")
}

android {
    namespace = "<namespace>"
}

dependencies {

    implementation(project(":resources:icon-pack"))
    implementation(project(":resources:string-resources"))
    implementation(project(":domain"))

    //core components
    implementation(lib.mega.core.ui)
    implementation(lib.mega.core.ui.tokens)

    // Compose
    implementation(androidx.bundles.compose.bom)
    implementation(androidx.material3)
    implementation(androidx.material3.adaptive)
    implementation(androidx.material3.adaptive.layout)
    implementation(androidx.material3.adaptive.navigation)
    implementation(androidx.material3.window)
    implementation(lib.coil3)
    implementation(lib.coil.compose)

    //test
    testImplementation(project(":core-test"))
    testImplementation(project(":core-ui-test"))
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
```

### Core Module

```kotlin
plugins {
    alias(convention.plugins.mega.android.library)
}

android {
    namespace = "<namespace>"
}

dependencies {
    implementation(project(":domain"))
}
```

### Shared Module

```kotlin
plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.library.compose)
    alias(convention.plugins.mega.android.hilt)
    id("kotlin-android")
}

android {
    namespace = "<namespace>"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":resources:string-resources"))

    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}
```

---

## Plugin Selection

Default plugins by module type:

| Plugin | Feature (simple/nested) | Snowflakes | Core | Shared |
|--------|:-----------------------:|:----------:|:----:|:------:|
| `mega.android.library` | Yes |    Yes     | Yes | Yes |
| `mega.android.library.compose` | Yes |    Yes     | No | Yes |
| `mega.android.hilt` | Yes |    Yes     | No | Yes |
| `kotlin.serialisation` | Yes |    Yes     | No | No |
| `kotlin-android` | Yes |    Yes     | No | Yes |

Optional plugins (added via `--plugins`):

| Plugin | Alias/ID |
|--------|----------|
| Room | `alias(convention.plugins.mega.android.room)` |
| Parcelize | `id("kotlin-parcelize")` |
| Serialisation | `alias(plugin.plugins.kotlin.serialisation)` |

---

## Namespace Rules

The `namespace` in the `android {}` block must be unique across all modules.

1. **Feature modules** (simple or nested main): `mega.privacy.android.feature.{name}` where `{name}` has hyphens removed (e.g., `cloud-drive` → `clouddrive`, `text-editor` → `texteditor`)
2. **Snowflakes modules**: `mega.privacy.android.feature.{name}.components` where `{name}` has hyphens removed
3. **Core modules**: `mega.privacy.android.core.{name}` where `{name}` has hyphens replaced with underscores (e.g., `my-utility` → `mega.privacy.android.core.my_utility`)
4. **Shared modules**: `mega.privacy.android.shared.{name}` where `{name}` has hyphens removed (e.g., `my-shared` → `mega.privacy.android.shared.myshared`)

When in doubt about namespace, check existing modules in the same category:
```
Grep: namespace = .*mega.privacy.android
Glob: {category}/*/*.gradle.kts
```

---

## Guidelines

1. **Always confirm before creating** — present the full creation plan and wait for user approval before writing any files.
2. **Use existing modules as reference** — when in doubt about a convention, search for similar existing modules and follow their patterns.
3. **Kebab-case module names** — module directory names must use kebab-case (e.g., `my-feature`, not `myFeature` or `my_feature`).
4. **Source directory is `src/main/java/`** — this project uses `java/` as the source root even for Kotlin files. Do NOT use `src/main/kotlin/`.
5. **Build file naming** — each module's build file must be named `{module-name}.gradle.kts` (not `build.gradle.kts`). This is enforced by the `rootProject.children.forEach` block in `settings.gradle.kts`.
6. **Minimal dependencies** — start with minimal dependencies from the templates. The user can add more as needed. Do not copy the full dependency list from existing feature modules.
7. **No proguard files** — `consumer-rules.pro` and `proguard-rules.pro` are not required for new modules.
8. **Nested vs simple** — if the user asks for a feature module without specifying, default to simple. Suggest nested only if they mention needing reusable snowflake UI components.
9. **No arbitrary nesting** — only the feature snowflake pattern is supported as nested. Do not create modules inside existing parent modules like `core/ui-components/` or `core/analytics/`.