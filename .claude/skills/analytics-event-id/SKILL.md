---
name: analytics-event-id
description: >
  Calculate the numeric MEGA analytics event ID for Android and iOS from an event name, and
  decode an ID seen in logs back to its event name. Reads IDs from the resolved
  analytics-events artifact first, falling back to a mobile-analytics checkout or GitLab.
triggers:
  - /analytics-event-id
  - what is the event id for
  - what event is 302137
  - calculate the analytics event value
---

# Analytics Event ID

Resolve `event name → numeric ID` (and back) for MEGA's analytics events, per platform.

Use this when verifying tracking against `adb logcat` output (which prints
`Event <id>: <EventName> {...}`), when filling the "Event name/code" column of a tracking
spec, or when a QA/analytics report references a bare numeric ID.

## Where the IDs come from

**The resolved dependency is checked first**, then a checkout as fallback. In order:

1. **The `analytics-events-android` artifact in the Gradle cache.** This is what the app
   actually builds against, so its IDs cannot be stale or come from someone's feature branch —
   the most trustworthy source, and Android devs already have it with no extra setup.
   The artifact ships compiled classes but **not** the JSON maps, so IDs are recovered from
   each generated class's bytecode. Indexing takes ~3s once per artifact version, then it is
   cached under `~/.cache/mega-analytics-event-id/` and lookups are instant.
2. **A mobile-analytics checkout** — used for anything the dependency does not answer (a
   small number of events are not recoverable from bytecode; see Caveats), and as the whole
   source when there is no artifact. Located via `--repo`, `$MOBILE_ANALYTICS_REPO`, then
   common paths (`~/StudioProjects`, `~/Projects`, `~/dev`, `~/src`, `~`, `../`, `./`).
3. **GitLab** — see [When there is no local checkout](#when-there-is-no-local-checkout).

An explicit `--json-dir` / `--repo` is authoritative: it skips the dependency, and a path that
does not exist is an **error** rather than being silently replaced by a fallback, because
resolving against the wrong checkout would publish wrong IDs.

Output names its source per query, e.g. `[via dependency 20260805.035218]` or `[via checkout]`.

Setup is usually nothing at all on Android. Otherwise:

```bash
export MOBILE_ANALYTICS_REPO=/path/to/mobile-analytics
```

Indexing the dependency needs `javap` (any JDK). Without it, the script says so and falls back.

Requires Python 3.6+, no third-party packages. `SCRIPT` below is
`<skill-dir>/scripts/event_id.py` — the script has no working-directory dependency, so an
absolute path works from anywhere.

## Usage

```bash
# name → ID (defaults to Android + iOS)
python3 "$SCRIPT" UpgradeAccountPlanScreen

# the generated Kotlin object name works too — the trailing `Event` is stripped
python3 "$SCRIPT" BuyProLiteEvent

# ID → name
python3 "$SCRIPT" 302137

# several at once, names and IDs mixed
python3 "$SCRIPT" BuyProI 300027 EndCallForAll

# just one platform
python3 "$SCRIPT" --platform ios BuyProLite
```

Options:

| Option | Default | Purpose |
|---|---|---|
| `--repo <path>` | *(autodiscovered)* | mobile-analytics checkout; skips the dependency |
| `--json-dir <path>` | *(derived from `--repo`)* | directory holding the `*Event.json` maps |
| `--no-dependency` | off | ignore the Gradle-cache artifact, read a checkout instead |
| `--platform <name>` | `android` and `ios` | `android` / `ios` / `macos` / `windows`; repeatable |
| `--app-identifier <0-9>` | `0` | `AppIdentifier` of the consuming app |

Exit code is `1` if any name or ID could not be resolved, `2` if no JSON maps were found.

The header line reports how many of the 11 maps were loaded (`3/11 files`). A partial set is
fine when you know the event's type, but a `NOT FOUND` against a partial set may just mean the
relevant map is missing — the script says so explicitly.

## The formula

From `analytics-core/src/commonMain/kotlin/.../event/type/AnalyticsEvent.kt:18`:

```
eventId = platformBase + eventTypeOffset + uniqueId + appIdentifier * 10_000
```

**Platform base** — `analytics-core/src/<target>Main/.../Platform.<target>.kt`:

| Platform | Base |
|---|---|
| Android | `300_000` |
| iOS | `400_000` |
| macOS | `400_000` (same as iOS) |
| Windows | `700_000` |

**Event type offset** — `analytics-core/src/commonMain/kotlin/.../event/type/*.kt`:

| Type | Offset | | Type | Offset |
|---|---|---|---|---|
| `ScreenViewEvent` | 0 | | `MenuItemEvent` | 5000 |
| `TabSelectedEvent` | 1000 | | `NotificationEvent` | 6000 |
| `ButtonPressEvent` | 2000 | | `GeneralEvent` | 7000 |
| `DialogDisplayedEvent` | 3000 | | `ItemSelectedEvent` | 8000 |
| `NavigationEvent` | 4000 | | `GestureEvent` | 9000 |

> The repo's own `CLAUDE.md` omits `GestureEvent`; 9000 is read from `GestureEvent.kt:17`.

**App identifier** — `AppIdentifier(id)` requires `id in 0..9` and contributes `id * 10_000`.
The MEGA Android app passes `AppIdentifier(0)`, so it contributes nothing
(`core/analytics/analytics-tracker/.../di/AnalyticsModule.kt:29`).

**`uniqueId`** comes from the KSP-generated map for the event's type,
`shared/src/commonMain/resources/<Type>Event.json`.

### Worked example

`UpgradeAccountPlanScreen` is in `ScreenViewEvent.json` with `uniqueId` 27:

- Android: `300000 + 0 + 27` = **300027** → logcat prints `Event 300027: UpgradeAccountPlanScreen {}`
- iOS: `400000 + 0 + 27` = **400027**

`BuyProLite` is in `ButtonPressEvent.json` with `uniqueId` 137:

- Android: `300000 + 2000 + 137` = **302137**
- iOS: `400000 + 2000 + 137` = **402137**

## Name lookup rule

JSON keys are the **annotated interface** names. KSP generates a Kotlin object that appends
`Event`, and the name printed in logcat is the interface name.

```
interface BuyProLite          →  object BuyProLiteEvent      →  JSON key "BuyProLite"
```

So `BuyProLite`, `BuyProLiteEvent` and the logcat name all resolve to the same entry. No JSON
key ends in `Event`, so stripping a trailing `Event` is always safe.

## When there is no local checkout

The maps live in the `mobile-analytics` repo at
`shared/src/commonMain/resources/<Type>Event.json`
(https://code.developers.mega.co.nz/mobile/kmm/mobile-analytics).

Fetch the file for the event's type off `main`, save it into a scratch directory, and point the
script at it with `--json-dir`. Prefer the GitLab MCP tool, which already has auth:

```
mcp__gitlab__get_file_contents
  project_id: mobile/kmm/mobile-analytics
  file_path:  shared/src/commonMain/resources/ButtonPressEvent.json
  ref:        main
```

Write the `content` field verbatim to `<scratch>/ButtonPressEvent.json`. If the event type is
unknown, fetch all 11 maps — the script searches every map it finds and reports which one matched.

Fallback without MCP:

```bash
curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  "https://code.developers.mega.co.nz/api/v4/projects/mobile%2Fkmm%2Fmobile-analytics/repository/files/shared%2Fsrc%2FcommonMain%2Fresources%2FButtonPressEvent.json/raw?ref=main"
```

## Verifying against a device (Android)

```bash
adb logcat -d -v time | grep -aE "megaJNI.*Event [0-9]+:"
```

Each event is logged twice — once through `megaJNI` and once through `megachatJNI`. That is the
two SDK loggers, **not** two events; filter to one of them before counting. Lines in the `99xxx`
range with array/JSON payloads are MEGA SDK telemetry, not app analytics.

On other platforms the log plumbing differs (iOS/macOS surface SDK logs through the Apple
unified log rather than logcat), but the printed form is the same — `Event <id>: <EventName>
<payload>` — so the decode direction of this script works on any platform's logs.

## Sharing this skill with other teams

The skill is **platform-agnostic**: the uniqueIds live in the shared KMP module, so iOS, macOS
and Windows resolve through the same maps and differ only by platform base. Nothing in the
script is Android-specific.

To install it in another repo, copy the whole directory:

```
<that-repo>/.claude/skills/analytics-event-id/
    SKILL.md
    scripts/event_id.py
```

Then set `MOBILE_ANALYTICS_REPO`, or pass `--repo`. Nothing else needs changing. The script can
equally be run standalone, without Claude Code, as a plain CLI.

Two things a non-Android reader should know:

- The **"Verifying against a device"** section above is Android-only.
- The `app identifier` note below is verified for the MEGA **Android** app only. Each consuming
  app supplies its own, so confirm yours before trusting cross-platform numbers.

## Caveats

- **Legacy events are not offset.** `LegacyEvent.getEventIdentifier()` returns the raw
  `uniqueId` with no platform base, type offset or app identifier
  (`LegacyEvent.kt`). IDs in `LegacyEvent.json` (e.g. `EndCallForAll` = 99309) are therefore
  identical on every platform, and cannot be decoded per-platform.
- **iOS app identifier is assumed to be 0.** Only the Android app's `AppIdentifier(0)` is
  verified in this repo. If the iOS app passes a different id, its real IDs are higher by
  `id * 10_000` — confirm on the iOS side before publishing iOS numbers, or pass
  `--app-identifier`.
- **A `uniqueId` ≥ 1000 overflows** into the next event-type bucket, because the type offsets are
  1000 apart. The script warns when it sees one. Current maps are well under the limit
  (`ButtonPressEvent.json` is the largest at ~468 entries).
- **Unmerged events have no ID yet.** IDs are assigned by KSP at build time and committed in the
  JSON. An event that is only an annotation in a branch resolves to `NOT FOUND`; run
  `./gradlew build` in mobile-analytics and commit the regenerated JSON. Predicted "next free"
  numbers are guesses only, and shift if another events MR merges to `main` first.
- **A local checkout may be stale or on a feature branch.** For authoritative numbers, either
  `git fetch && git checkout origin/main` first, or fetch the JSON from GitLab at `ref: main`.
- **Decoding is ambiguous across platforms** when a bare ID could belong to more than one
  platform/type combination. The script lists every match rather than guessing.
- **A few events are not recoverable from the dependency.** The generated classes normally
  assign `eventName` and `uniqueIdentifier` as constants the disassembler can read, but a
  minority do not (e.g. where the value is the JVM default and no assignment is emitted).
  Those are omitted from the dependency index rather than guessed — so the script falls back
  to the checkout for them, and answers `NOT FOUND` if no checkout is available. It never
  invents a value. Measured against a full checkout: 1201 of 1271 events resolved from the
  artifact with **zero** mismatches; the remainder came from the checkout.
- **The dependency can legitimately be older than `main`.** That is the point — it reflects
  what the app ships. If you need a not-yet-released event, use the checkout or GitLab at
  `ref: main`, and expect the ID to be absent until the events MR is merged and published.
