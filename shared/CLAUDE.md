# CLAUDE.md

This file provides guidance to Claude Code when working in any `:shared:*` module.
It complements the per-module `CLAUDE.md` files (e.g. `shared/nodes/CLAUDE.md`)
and the root `.claude/CLAUDE.md`; read those for module internals and global rules.

## Purpose
`shared/` holds presentation-layer logic, Compose UI, and mappers that are reused
by **more than one** feature (e.g. `nodes`, `account`, `transfers`, `chats`,
`contact`, `search`, `ads`, `sync`). Shared modules sit between `core/` and
`feature/`: richer than core primitives, but not owned by any single feature.

## What belongs here / What doesn't
- **Belongs here:** code used by **2+ features**. This is the defining test — a
  shared module exists precisely because multiple features need the same thing.
- **Doesn't belong here:** anything used by only one feature — keep that in the
  feature (or its sibling SnowFlake). Don't pre-emptively "share" single-feature
  code.
- **SnowFlake-wrapper role:** a `:shared:*` module is the correct way to expose a
  single-consumer SnowFlake's composables to more than one consumer — wrap the
  SnowFlake behind a shared interface and depend on it from the shared module.
- Domain logic (use cases, repository interfaces) belongs in `:domain`; shared
  modules *consume* it.

## Structure & layout
- `shared/{name}/{name}.gradle.kts`, sources under
  `src/main/java/mega/privacy/android/shared/{name}/` (typically `components/`,
  `mapper/`, `model/`, `dialog/`, `sheet/`, etc.).
- `:shared:original-core-ui` is legacy — an architecture **exception** to be
  replaced by the core-ui library; don't build new UI on top of it.

## Naming & namespaces
- Path `:shared:{name}`; build file `{name}.gradle.kts`.
- Namespace `mega.privacy.android.shared.{name}` with hyphens removed.

## Plugins
Typical set: `mega.android.library`, `mega.android.library.compose`,
`mega.android.hilt`, `kotlin-android` (add `mega.android.room` /
`kotlin.serialisation` / `kotlin-parcelize` when needed).

## Dependency rules (enforced)
Enforced at build time by `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`;
a violation fails the build with a `GradleException`.

- A Shared module **MAY** depend on: `:core:*`, SnowFlake modules, and
  `:resources:*`.
- A Shared module **MUST NOT** depend on any `:feature:*` module, nor on another
  `:shared:*` module.
- `:domain`, `:data`, `:navigation`, the MEGA SDK, etc. are **unclassified**, so
  depending on them is not restricted — shared modules commonly depend on
  `:domain`/`:data` because they serve multiple features; this is expected and not
  a loophole.
- **Design tokens:** direct use of `lib.mega.core.ui.tokens` should ideally be
  confined to SnowFlake modules, with other modules consuming core-ui composables.
  This is a convention (not yet lint-enforced); the current `:shared:*` modules
  reference tokens directly today — pre-existing debt being migrated, not a
  pattern to extend to new code.

A temporary allowlist in `ArchitecturePlugin.kt` exempts a few legacy modules
(e.g. `:shared:original-core-ui`) as tech debt. Don't add to it — new modules must
follow the rules above.

## Testing
JUnit 5 + Mockito + Turbine + Truth, plus Compose UI tests where applicable (test
deps typically include `:core-test`, `:core-ui-test`). Run with
`./gradlew shared:{name}:testDebugUnitTest`.

## Examples
- `:shared:nodes` — shared node list/grid/selection UI and domain→UI mappers used
  by cloud-drive, search, photos, pickers. See `shared/nodes/CLAUDE.md`.
- `:shared:account` — account UI (over-quota banners, login-in-progress state)
  consumed across features.

## Source of truth
- Enforced rules & layer detection: `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`.
- Creating a module: `.claude/skills/create-module/SKILL.md` (or `/create-module`).
- Global conventions (indentation, naming, testing): root `.claude/CLAUDE.md`.
