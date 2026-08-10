# CLAUDE.md

This file provides guidance to Claude Code when working in any `:core:*` module.
It complements the per-module `CLAUDE.md` files (e.g. `core/coroutine/CLAUDE.md`)
and the root `.claude/CLAUDE.md`; read those for module internals and global rules.

## Purpose
`core/` holds low-level, dependency-light infrastructure and design-system
primitives shared across the whole codebase — coroutine/`Flow` helpers, the
feature-flag catalog, formatters, navigation contracts, analytics, and reusable
UI-component building blocks. Core modules are foundational: they are consumed by
feature and shared modules but know nothing about any specific feature.

## What belongs here / What doesn't
- **Belongs here:** generic, cross-cutting utilities and primitives that are not
  tied to a single feature and carry no business logic of their own.
- **Doesn't belong here:** anything feature-specific (put it in `:feature:*`),
  cross-feature presentation reused by more than one feature (put it in
  `:shared:*`), or raw strings/icons (put them in `:resources:*`).
- If you find yourself importing `:domain` types to model a specific feature's
  behaviour, it probably belongs in a feature or shared module, not here.

## Structure & layout
- Simple module: `core/{name}/{name}.gradle.kts`, sources under
  `src/main/java/mega/privacy/android/core/{name}/`.
- Some `core/` directories contain **SnowFlake** sub-modules
  (`core/passcode/passcode-snowflake-components`,
  `core/navigation-snowflake-components`, `core/ui-components/*-snowflake-components`).
  Any module whose path ends in `-snowflake-components` / `-snowflakes` is a
  **SnowFlake layer** module — it follows the SnowFlake rules (see
  `feature/CLAUDE.md`), **not** the Core rules, even though it lives under `core/`.

## Naming & namespaces
- Module path `:core:{name}`; build file `{name}.gradle.kts`.
- Namespace `mega.privacy.android.core.{name}` with hyphens replaced by
  underscores (e.g. `feature-flags` → `mega.privacy.android.feature_flags`,
  `my-utility` → `mega.privacy.android.core.my_utility`).

## Plugins
Core modules are intentionally minimal — usually just
`alias(convention.plugins.mega.android.library)`. Add Compose/Hilt/Room/
serialisation only when a specific core module genuinely needs them (e.g.
`core/ui-components/*` add Compose). Prefer the smallest plugin set that works.

## Dependency rules (enforced)
Enforced at build time by `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`
(applied to every library via `AndroidLibraryConventionPlugin`); a violation
fails the build with a `GradleException`.

- A Core module **MAY** depend on: `:resources:*` and other `:core:*`.
- A Core module **MUST NOT** depend on: `:feature:*`, `:shared:*`, or SnowFlake
  modules.
- **Core→Core is discouraged.** The plugin still tolerates it (flagged
  to-be-removed), but the preference is that core modules do **not** depend on
  each other, and existing cases are actively being removed. Do not add new
  core-to-core coupling.
- `:domain`, `:data`, `:navigation`, `:core-test`, the MEGA SDK, etc. are
  **unclassified** by the plugin, so depending on them is not restricted. Core
  modules commonly depend on `:domain` for shared types; that is expected and not
  a loophole.
- **Design tokens:** direct use of `lib.mega.core.ui.tokens` should be confined to
  SnowFlake modules; other modules consume core-ui *composables* instead. This is
  a convention (not yet lint-enforced); the deprecated `:core:ui-components:*`
  modules still reference tokens directly and are pre-existing debt.

A temporary allowlist in `ArchitecturePlugin.kt` (`dependencyExceptions` /
`moduleExceptions`) exempts a few legacy modules as tech debt. Don't add to it —
new modules must follow the rules above.

## Testing
JUnit 5 + Mockito + Turbine + Truth (see root `.claude/CLAUDE.md`). Run a
module's unit tests with `./gradlew core:{name}:testDebugUnitTest`.

## Examples
- `:core:coroutine` — `Flow`/coroutine extension helpers; depends only on external
  libs (`kotlinx-coroutines`, Timber). See `core/coroutine/CLAUDE.md`.
- `:core:feature-flags` — app-wide feature-flag catalog; depends only on `:domain`.

## Source of truth
- Enforced rules & layer detection: `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`.
- Creating a module: `.claude/skills/create-module/SKILL.md` (or `/create-module`).
- Global conventions (indentation, naming, testing): root `.claude/CLAUDE.md`.
