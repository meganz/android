#!/usr/bin/env python3
"""Resolve MEGA analytics event IDs from event names, and decode IDs back to names.

Platform-agnostic: works for Android, iOS, macOS and Windows, because every platform
reads the same KSP-generated uniqueId maps from the shared mobile-analytics module.

Event ID formula (analytics-core/.../type/AnalyticsEvent.kt):

    platformBase + eventTypeOffset + uniqueId + appIdentifier * 10_000

`uniqueId` comes from shared/src/commonMain/resources/<Type>Event.json, keyed by the
*interface* name (the generated Kotlin object appends `Event`).

LegacyEvent overrides getEventIdentifier() and returns its raw uniqueId with no platform
base, type offset or app identifier applied.

Requires Python 3.6+. No third-party dependencies.
"""

import argparse
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

PLATFORM_BASE = {
    "android": 300_000,
    "ios": 400_000,
    "macos": 400_000,
    "windows": 700_000,
}

# Offsets read from analytics-core/src/commonMain/kotlin/.../event/type/*.kt
# Keyed by JSON filename so no string surgery is needed to map file -> offset.
TYPE_OFFSET = {
    "ScreenViewEvent.json": 0,
    "TabSelectedEvent.json": 1000,
    "ButtonPressEvent.json": 2000,
    "DialogDisplayedEvent.json": 3000,
    "NavigationEvent.json": 4000,
    "MenuItemEvent.json": 5000,
    "NotificationEvent.json": 6000,
    "GeneralEvent.json": 7000,
    "ItemSelectedEvent.json": 8000,
    "GestureEvent.json": 9000,
}

# Raw IDs, not offset by platform/type/app.
LEGACY_FILE = "LegacyEvent.json"

ALL_FILES = list(TYPE_OFFSET) + [LEGACY_FILE]

RESOURCE_SUBPATH = os.path.join("shared", "src", "commonMain", "resources")

# Checked in order when --repo/--json-dir are not given. MOBILE_ANALYTICS_REPO wins.
REPO_CANDIDATES = [
    "~/StudioProjects/mobile-analytics",
    "~/Projects/mobile-analytics",
    "~/dev/mobile-analytics",
    "~/src/mobile-analytics",
    "~/mobile-analytics",
    "../mobile-analytics",
    "./mobile-analytics",
]


# --- Source 1: the resolved dependency (preferred) --------------------------------
#
# The published artifact is what the app actually builds against, so its IDs cannot be
# stale or come from someone's feature branch. It ships compiled classes but NOT the JSON
# maps, so the ids are recovered from each generated class's static initialiser, which
# assigns both the event name and its uniqueId as constants.

ARTIFACT_GLOB = os.path.expanduser(
    "~/.gradle/caches/modules-2/files-2.1/mega.privacy.mobile/"
    "analytics-events-android/*/*/analytics-events-android-*.aar"
)

EVENT_CLASS_RE = re.compile(
    r"^mega/privacy/mobile/analytics/event/([A-Za-z0-9_]+Event)\.class$"
)
# `implements ...core.event.identifier.<Type>Identifier` -> event type
IMPLEMENTS_RE = re.compile(r"identifier\.([A-Za-z]+)Identifier")
NAME_RE = re.compile(r'ldc\s+#\d+\s+// String ([A-Za-z0-9_]+)')
UNIQUE_ID_RE = re.compile(r"(?:sipush|bipush)\s+(\d+)|iconst_(\d)")

# javap identifier interface -> our JSON-style type file, so both sources share one shape.
IDENTIFIER_TO_FILE = {
    "ScreenViewEvent": "ScreenViewEvent.json",
    "TabSelectedEvent": "TabSelectedEvent.json",
    "ButtonPressedEvent": "ButtonPressEvent.json",
    "DialogDisplayedEvent": "DialogDisplayedEvent.json",
    "NavigationEvent": "NavigationEvent.json",
    "MenuItemEvent": "MenuItemEvent.json",
    "NotificationEvent": "NotificationEvent.json",
    "GeneralEvent": "GeneralEvent.json",
    "ItemSelectedEvent": "ItemSelectedEvent.json",
    "GestureEvent": "GestureEvent.json",
}


def newest_artifact():
    """Newest cached analytics-events-android AAR, or None."""
    matches = glob.glob(ARTIFACT_GLOB)
    if not matches:
        return None
    # Version is the directory two levels above the file and is timestamp-ordered.
    return max(matches, key=lambda p: p.split(os.sep)[-3])


def artifact_version(aar_path):
    return aar_path.split(os.sep)[-3]


def cache_path(version):
    base = os.environ.get(
        "XDG_CACHE_HOME", os.path.expanduser("~/.cache")
    )
    return os.path.join(base, "mega-analytics-event-id", version + ".json")


def build_index_from_aar(aar_path, verbose=True):
    """Return {type_file: {event_name: unique_id}} decoded from the AAR's classes.

    Cached per artifact version, because a full disassembly takes a few seconds.
    """
    version = artifact_version(aar_path)
    cached = cache_path(version)
    if os.path.isfile(cached):
        with open(cached) as handle:
            return json.load(handle)

    if not shutil.which("javap"):
        if verbose:
            print(
                "javap not found (needs a JDK) — cannot read the dependency; "
                "falling back to a mobile-analytics checkout.",
                file=sys.stderr,
            )
        return None

    workdir = tempfile.mkdtemp(prefix="analytics-event-id-")
    try:
        with zipfile.ZipFile(aar_path) as aar:
            aar.extract("classes.jar", workdir)
        jar = os.path.join(workdir, "classes.jar")

        with zipfile.ZipFile(jar) as classes:
            names = [
                m.group(1)
                for m in (EVENT_CLASS_RE.match(n) for n in classes.namelist())
                if m
            ]
        if not names:
            return None

        if verbose:
            print(
                "indexing {} events from dependency {} (first run only)".format(
                    len(names), version
                ),
                file=sys.stderr,
            )

        index = {}
        prefix = "mega.privacy.mobile.analytics.event."
        # Chunked: 1200+ class names can exceed the shell/exec argument limit.
        for start in range(0, len(names), 150):
            chunk = [prefix + n for n in names[start:start + 150]]
            # List form, so no shell is involved; EVENT_CLASS_RE has already restricted
            # every name to [A-Za-z0-9_]+. The timeout guards against a malformed jar
            # making javap hang rather than fail.
            try:
                out = subprocess.run(
                    ["javap", "-c", "-p", "-cp", jar] + chunk,
                    capture_output=True,
                    text=True,
                    timeout=120,
                ).stdout
            except subprocess.TimeoutExpired:
                print(
                    "javap timed out reading the dependency; falling back to a checkout.",
                    file=sys.stderr,
                )
                return None
            _parse_javap(out, index)

        if index:
            os.makedirs(os.path.dirname(cached), exist_ok=True)
            with open(cached, "w") as handle:
                json.dump(index, handle)
        return index or None
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


def _parse_javap(output, index):
    """Fold `javap -c -p` output into {type_file: {name: uniqueId}}.

    Simple events compile to an object assigning static fields in `static {}`; events with
    runtime parameters compile to a data class assigning instance fields in `<init>`. Rather
    than guessing by position, each `putstatic`/`putfield` of `eventName` / `uniqueIdentifier`
    is paired with the constant pushed on the line immediately before it, which covers both.

    This depends on `javap -c` instruction formatting, so it is coupled to the JDK. Verified
    against OpenJDK 21.0.10. If a JDK change alters that output the parse yields fewer events
    rather than wrong ones — unparsed classes are skipped, never guessed, and the checkout
    fallback then answers for them.
    """
    for block in output.split("Compiled from "):
        if "analytics.event." not in block:
            continue
        implements = IMPLEMENTS_RE.search(block)
        if not implements:
            continue
        type_file = IDENTIFIER_TO_FILE.get(implements.group(1))
        if type_file is None:
            continue

        name = None
        unique_id = None
        previous = ""
        for line in block.splitlines():
            if "Field eventName:" in line and ("putstatic" in line or "putfield" in line):
                match = NAME_RE.search(previous)
                if match:
                    name = match.group(1)
            elif "Field uniqueIdentifier:" in line and (
                "putstatic" in line or "putfield" in line
            ):
                match = UNIQUE_ID_RE.search(previous)
                if match:
                    unique_id = int(
                        match.group(1) if match.group(1) is not None else match.group(2)
                    )
            previous = line

        if name is not None and unique_id is not None:
            index.setdefault(type_file, {})[name] = unique_id


def type_label(type_file):
    """'ButtonPressEvent.json' -> 'ButtonPressEvent' (no str.removesuffix; 3.6 compatible)."""
    return type_file[: -len(".json")] if type_file.endswith(".json") else type_file


def load_maps(json_dir):
    """Return {type_file: {event_name: unique_id}} for every JSON map found."""
    maps = {}
    for type_file in ALL_FILES:
        path = os.path.join(json_dir, type_file)
        if os.path.isfile(path):
            with open(path) as handle:
                maps[type_file] = json.load(handle)
    return maps


def discover_json_dir(args):
    """Resolve the directory holding the *Event.json maps, or None.

    An explicitly supplied --json-dir/--repo is never silently replaced by a fallback:
    resolving a bad explicit path against some other checkout could publish IDs from the
    wrong repo. Autodiscovery only runs when neither flag was given.
    """
    if args.json_dir:
        expanded = os.path.expanduser(args.json_dir)
        return expanded if os.path.isdir(expanded) else None

    if args.repo:
        resolved = os.path.join(os.path.expanduser(args.repo), RESOURCE_SUBPATH)
        return resolved if os.path.isdir(resolved) else None

    candidates = []
    env_repo = os.environ.get("MOBILE_ANALYTICS_REPO")
    if env_repo:
        candidates.append(env_repo)
    candidates.extend(REPO_CANDIDATES)

    for repo in candidates:
        resolved = os.path.join(os.path.expanduser(repo), RESOURCE_SUBPATH)
        if os.path.isdir(resolved):
            return resolved
    return None


def normalise(name):
    """JSON keys are interface names; generated objects append `Event`."""
    if name.endswith("Event"):
        return [name, name[: -len("Event")]]
    return [name, name + "Event"]


def lookup_name(maps, name):
    """Return list of (type_file, matched_key, unique_id) for a name."""
    hits = []
    for type_file, mapping in maps.items():
        for variant in normalise(name):
            if variant in mapping:
                hits.append((type_file, variant, mapping[variant]))
                break
    return hits


def event_id(type_file, unique_id, platform, app_identifier):
    if type_file == LEGACY_FILE:
        return unique_id
    return (
        PLATFORM_BASE[platform]
        + TYPE_OFFSET[type_file]
        + unique_id
        + app_identifier * 10_000
    )


def decode(maps, value, app_identifier, platforms):
    """Return list of (platform, type_file, name, unique_id) matching an ID."""
    results = []
    for name, unique_id in maps.get(LEGACY_FILE, {}).items():
        if unique_id == value:
            results.append(("any (legacy)", LEGACY_FILE, name, unique_id))

    for platform in platforms:
        remainder = value - PLATFORM_BASE[platform] - app_identifier * 10_000
        if remainder < 0:
            continue
        for type_file, mapping in maps.items():
            if type_file == LEGACY_FILE:
                continue
            unique_id = remainder - TYPE_OFFSET[type_file]
            if not 0 <= unique_id < 1000:
                continue
            for name, candidate in mapping.items():
                if candidate == unique_id:
                    results.append((platform, type_file, name, unique_id))
    return results


def main():
    """Entry point. Returns 0 on success, 1 if any query was unresolved, 2 if no source."""
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "queries",
        nargs="+",
        help="Event names (BuyProLite, BuyProLiteEvent) and/or numeric IDs (302137)",
    )
    parser.add_argument(
        "--repo",
        help="mobile-analytics checkout (else $MOBILE_ANALYTICS_REPO, else common locations)",
    )
    parser.add_argument("--json-dir", help="Directory holding the *Event.json maps")
    parser.add_argument(
        "--no-dependency",
        action="store_true",
        help="Skip the resolved dependency and read a mobile-analytics checkout instead",
    )
    parser.add_argument(
        "--platform",
        action="append",
        choices=sorted(PLATFORM_BASE),
        help="Restrict output to this platform; repeatable. Default: android and ios.",
    )
    parser.add_argument(
        "--app-identifier",
        type=int,
        default=0,
        help="AppIdentifier id 0-9; MEGA mobile uses 0 (default)",
    )
    args = parser.parse_args()

    if not 0 <= args.app_identifier <= 9:
        parser.error("--app-identifier must be in 0..9")

    platforms = args.platform or ["android", "ios"]

    # Dependency first: the resolved artifact is exactly what the app builds against, so it
    # cannot be stale or come from someone's feature branch. A handful of events cannot be
    # recovered from bytecode (see build_index_from_aar), so a checkout, when present, is
    # consulted for anything the dependency does not answer.
    sources = []
    explicit_source = bool(args.json_dir or args.repo)
    if not explicit_source and not args.no_dependency:
        aar = newest_artifact()
        if aar:
            index = build_index_from_aar(aar)
            if index:
                total = sum(len(v) for v in index.values())
                sources.append(
                    ("dependency " + artifact_version(aar), index, [])
                )
                print("source: dependency {} ({} events)".format(
                    artifact_version(aar), total))

    json_dir = discover_json_dir(args)
    if json_dir is None and sources:
        # Dependency answered; no checkout available for fallback.
        return report(args, sources, platforms)

    if json_dir is None:
        looked = args.json_dir or args.repo or "$MOBILE_ANALYTICS_REPO / common locations"
        print(
            "No JSON maps found (looked in: {}).\n\n"
            "Fix by any one of:\n"
            "  export MOBILE_ANALYTICS_REPO=/path/to/mobile-analytics\n"
            "  {} --repo /path/to/mobile-analytics ...\n"
            "  {} --json-dir /path/with/EventMaps ...\n\n"
            "The maps live at {}/<Type>Event.json in\n"
            "https://code.developers.mega.co.nz/mobile/kmm/mobile-analytics".format(
                looked, sys.argv[0], sys.argv[0], RESOURCE_SUBPATH
            ),
            file=sys.stderr,
        )
        return 2

    maps = load_maps(json_dir)
    if not maps:
        print(
            "No *Event.json files in {}. Expected one or more of: {}".format(
                json_dir, ", ".join(ALL_FILES)
            ),
            file=sys.stderr,
        )
        return 2

    missing = [f for f in ALL_FILES if f not in maps]
    print("source: checkout {} ({}/{} maps){}".format(
        json_dir, len(maps), len(ALL_FILES),
        " — fallback" if sources else ""))
    sources.append(("checkout", maps, missing))

    return report(args, sources, platforms)


def report(args, sources, platforms):
    """Print results for every query, trying each source in order."""
    print("platforms: {}".format(", ".join(platforms)))
    if args.app_identifier:
        print("app identifier: {} (+{})".format(
            args.app_identifier, args.app_identifier * 10_000))
    print()

    multi = len(sources) > 1
    exit_code = 0
    for query in args.queries:
        if query.lstrip("-").isdigit():
            matches, origin = [], None
            for label, maps, _ in sources:
                matches = decode(maps, int(query), args.app_identifier, platforms)
                if matches:
                    origin = label
                    break
            if not matches:
                print("{}: no event matches this ID".format(query))
                exit_code = 1
                continue
            print("{} decodes to:{}".format(
                query, "   [via {}]".format(origin) if multi else ""))
            for platform, type_file, name, unique_id in matches:
                print("  {:14} {}  ({}, uniqueId {})".format(
                    platform, name, type_label(type_file), unique_id))
            print()
            continue

        hits, origin, missing = [], None, []
        for label, maps, source_missing in sources:
            hits = lookup_name(maps, query)
            if hits:
                origin = label
                break
            missing = source_missing
        if not hits:
            print("{}: NOT FOUND in {}".format(
                query, " or ".join(label for label, _, _ in sources)))
            if missing:
                print("  {} map(s) not loaded — the event's type may be among them: {}".format(
                    len(missing), ", ".join(missing)))
            print("  Otherwise: the annotation is not yet merged to main, or the JSON was")
            print("  not regenerated (run ./gradlew build in mobile-analytics and commit it).")
            exit_code = 1
            continue

        if multi:
            print("[via {}]".format(origin))
        for type_file, key, unique_id in hits:
            print("{}  ({}, uniqueId {})".format(key, type_label(type_file), unique_id))
            if type_file == LEGACY_FILE:
                print("  all platforms  {}   (legacy: raw ID, no offsets)".format(unique_id))
            else:
                offset = TYPE_OFFSET[type_file]
                for platform in platforms:
                    total = event_id(type_file, unique_id, platform, args.app_identifier)
                    parts = "{} + {} + {}".format(
                        PLATFORM_BASE[platform], offset, unique_id)
                    if args.app_identifier:
                        parts += " + {}".format(args.app_identifier * 10_000)
                    print("  {:14} {}   ({})".format(platform, total, parts))
                if unique_id >= 1000:
                    print(
                        "  WARNING uniqueId {} >= 1000 — it overflows into the next"
                        " event-type bucket; report this to the analytics owners.".format(
                            unique_id)
                    )
        print()

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
