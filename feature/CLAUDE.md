# CLAUDE.md

This file provides guidance to Claude Code when working in any `:feature:*` module.
It complements the per-module `CLAUDE.md` files (e.g. `feature/sync/CLAUDE.md`)
and the root `.claude/CLAUDE.md`; read those for module internals and global rules.

## Purpose
`feature/` holds self-contained, user-facing features (chat, home, sync,
cloud-drive, photos, transfers, etc.). Features are the top of the module graph:
they compose primitives from `:core:*`, reusable UI from `:shared:*`, and their
own SnowFlake sub-modules into complete screens and flows. Some are
presentation-only (consume `:domain` use cases); others are full-stack with their
own data/domain/presentation layers.

## What belongs here / What doesn't
- **Belongs here:** everything specific to one feature — screens, ViewModels,
  navigation, feature-specific use cases and mappers.
- **A feature MUST NEVER depend on another feature.** If two features need the
  same code:
  - extract cross-feature presentation/logic into a `:shared:*` module, or
  - if it's reusable UI needed by only *one* feature, keep it in that feature's
    sibling SnowFlake module.
- Generic, feature-agnostic utilities belong in `:core:*`; raw strings/icons in
  `:resources:*`.

## Structure & layout
- **Simple feature:** `feature/{name}/{name}.gradle.kts` → `:feature:{name}`.
- **Nested feature:** `feature/{name}/{name}/` (main, `:feature:{name}:{name}`)
  plus one or more sibling SnowFlake modules
  `feature/{name}/{name}-snowflake-components/` (or `-snowflakes/`).
- Sources under `src/main/java/mega/privacy/android/feature/{name}/` (this project
  uses `java/` as the source root even for Kotlin).

### The SnowFlake layer
MEGA's UI is built from the shared `lib.mega.core.ui` (core-ui) library, which
internally uses a **design-token** system (`lib.mega.core.ui.tokens`). Ordinary
modules consume core-ui *composables* and do **not** reach for raw tokens. When a
feature needs a feature-specific composable that isn't generic enough to live in
core-ui but still needs token access, that composable goes in a **SnowFlake
module** — the sanctioned home for token access.

- A module is a SnowFlake when its path ends in `-snowflake-components` or
  `-snowflakes` (detected by name — this wins over the `core/`/`feature/`
  location).
- **Single-consumer rule:** a SnowFlake may be a dependency of exactly **one**
  other module — its sibling (`X-snowflake-components` is valid only for module
  `X`). This is build-enforced.
- **Escape hatch:** if more than one module needs a SnowFlake's composables, wrap
  it behind a `:shared:*` module that exposes an interface and itself depends on
  the SnowFlake. The shared module becomes the multi-consumer surface; the
  SnowFlake stays single-consumer.

## Naming & namespaces
- Path `:feature:{name}` (or `:feature:{name}:{name}` nested); build file
  `{name}.gradle.kts`.
- Namespace `mega.privacy.android.feature.{name}` with hyphens removed
  (`cloud-drive` → `mega.privacy.android.feature.clouddrive`). SnowFlake
  namespace: `mega.privacy.android.feature.{name}.components`.

## Plugins
Typical feature set: `mega.android.library`, `mega.android.library.compose`,
`mega.android.hilt`, `kotlin.serialisation`, `kotlin-android` (add
`mega.android.room` / `kotlin-parcelize` when needed). Feature modules wire into
navigation via a Hilt `@IntoSet FeatureDestination` (see `/create-module`).

## Dependency rules (enforced)
Enforced at build time by `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`;
a violation fails the build with a `GradleException`.

- A Feature module **MAY** depend on: `:core:*`, `:shared:*`, its sibling
  SnowFlake, and `:resources:*`.
- A Feature module **MUST NOT** depend on another `:feature:*` module.
- `:domain`, `:data`, `:navigation`, the MEGA SDK, etc. are **unclassified**, so
  depending on them is not restricted — features pull `:domain`/`:data` freely by
  design; this is not a loophole.
- **Design tokens:** direct use of `lib.mega.core.ui.tokens` should be confined to
  SnowFlake modules; plain feature modules should consume core-ui composables
  instead. This is a convention (not yet lint-enforced); a couple of features
  (`:feature:share-link`, `:feature:document-scanner`) still reference tokens
  directly and are pre-existing debt being migrated.

A temporary allowlist in `ArchitecturePlugin.kt` exempts a few legacy modules
(e.g. `:feature:sync`) as tech debt. Don't add to it — new modules must follow
the rules above.

## Testing
JUnit 5 + Mockito + Turbine + Truth (see root `.claude/CLAUDE.md`); Compose UI /
screenshot tests where applicable. Run with
`./gradlew feature:{name}:{name}:testDebugUnitTest`
(or `feature:{name}:testDebugUnitTest` for simple features).

## Examples
- `:feature:home:home` — presentation-only dashboard; consumes `:domain` use
  cases, `:shared:nodes`, core components.
- `:feature:sync` — full-stack feature with its own data/domain/presentation (and
  a current architecture exception). See `feature/sync/CLAUDE.md`.

## Source of truth
- Enforced rules & layer detection: `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`.
- Creating a module: `.claude/skills/create-module/SKILL.md` (or `/create-module`).
- Global conventions (indentation, naming, testing): root `.claude/CLAUDE.md`.
