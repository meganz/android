# CLAUDE.md

This file provides guidance to Claude Code when working in any `:resources:*`
module. It complements the root `.claude/CLAUDE.md`; read that for global rules.

## Purpose
`resources/` holds pure, business-logic-free assets shared across the entire app:
translatable strings and the generated Compose icon pack. These modules sit at the
**base of the dependency graph** — imported by nearly everything, depending on
nothing.

Current modules:
- `:resources:string-resources` — the app's translatable strings (`strings_shared.xml`),
  which feed the Weblate upload flow. Pure resources, no code.
- `:resources:icon-pack` — a Compose icon library whose `IconPack` object is
  **code-generated from SVG sources** at build time (uses KotlinPoet in debug).

## What belongs here / What doesn't
- **Belongs here:** raw strings, icons/drawables, and other static assets that do
  not depend on any business logic and are safe to consume from any module.
- **Doesn't belong here:** any `:domain`/`:data`/business logic, feature-specific
  code, or composables beyond the icon assets themselves. If it needs a use case
  or a domain type, it belongs in another group.

## Structure & layout
- `resources/{name}/{name}.gradle.kts`.
- `string-resources` is a plain resource library (no Compose); `icon-pack` applies
  Compose and generates its icons from SVGs — treat the generated `IconPack` as
  build output, edit the SVG sources / generator rather than generated code.

## Naming & namespaces
- Path `:resources:{name}`; build file `{name}.gradle.kts`.
- Namespaces are historical and do **not** follow a `resources.` prefix:
  `:resources:string-resources` → `mega.privacy.android.shared.resources`;
  `:resources:icon-pack` → `mega.privacy.android.icon.pack`. Match the existing
  module's namespace rather than inventing a new pattern.

## Plugins
Minimal: `mega.android.library` (plus `mega.android.library.compose` for
`icon-pack`). No Hilt, no Room. Add `lintChecks(project(":lint"))`.

## Dependency rules (enforced)
Enforced at build time by `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`;
a violation fails the build with a `GradleException`.

- A Resources module **MUST NOT** depend on any other layer — it is the base of
  the graph. Allowed dependencies are external libraries only (annotations,
  Compose BOM, KotlinPoet for icon generation).
- Every other group (`:core:*`, `:shared:*`, `:feature:*`, SnowFlakes) may — and
  routinely does — depend on `:resources:*`.

## Testing
Resource modules generally have no unit tests. If a generator or helper warrants
one, use the project's JUnit 5 setup (see root `.claude/CLAUDE.md`).

## Examples
- `:resources:string-resources` — strings for translation; consumed everywhere,
  depends on nothing but `androidx.annotation`.
- `:resources:icon-pack` — generated Compose `IconPack`; the source of truth is the
  SVG inputs and the generator, not the generated Kotlin.

## Source of truth
- Enforced rules & layer detection: `build-logic/convention/src/main/kotlin/ArchitecturePlugin.kt`.
- String upload flow: the `/weblate` skill.
- Global conventions: root `.claude/CLAUDE.md`.
