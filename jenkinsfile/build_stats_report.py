#!/usr/bin/env python3
"""
Summarises the per-build stats that `common.recordBuildStats` uploads to Artifactory.

Every MR build writes one JSON file to
    <ARTIFACTORY_BASE_URL>/artifactory/android-mega/cicd/build-stats/<YYYY>/<MM>/<ts>-<build>-<mr>.json
holding the pipeline's queue wait, total duration and per-stage durations. This script downloads a
window of those files and prints the duration distributions, so a CI-time claim can be checked
against what the pipeline actually did rather than against a model.

The five stages run in parallel, each on its own agent, so the stage durations do not add up to the
pipeline duration: wall-clock is the longest stage plus the serial preparation. The report therefore
also prints how often each stage is the longest one and how much wall-clock would disappear if the
unit-test stage were free, which is the only quantity a unit-test change can move.

Usage:
    python3 jenkinsfile/build_stats_report.py [--months 2026/08 2026/09] [--days 30] [--json out.json]

With no --months the current and previous month are read. --days keeps only builds that ended
within that many days of the newest build in the window. Reads are anonymous; only the Python
standard library is used.
"""

import argparse
import concurrent.futures
import datetime
import json
import os
import re
import statistics
import sys
import urllib.request

REPO_PATH = "/artifactory/android-mega/cicd/build-stats/"

STAGE_KEYS = [
    "build_apk_ms",
    "unit_test_ms",
    "lint_ms",
    "weblate_gate_ms",
    "code_review_ms",
]

CORE_STAGE_KEYS = ["build_apk_ms", "unit_test_ms", "lint_ms"]


def base_url():
    url = os.environ.get("ARTIFACTORY_BASE_URL")
    if not url:
        sys.exit("ARTIFACTORY_BASE_URL is not set")
    return url.rstrip("/")


def read(url):
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()


def list_month(base, month):
    """File names in a <YYYY>/<MM> folder, read out of Artifactory's HTML directory listing.

    The storage API needs a token; the listing does not, and a folder holds one flat set of files.
    """
    try:
        html = read("%s%s%s/" % (base, REPO_PATH, month)).decode()
    except Exception as error:  # a month that has not started yet has no folder
        print("  %s: %s" % (month, error), file=sys.stderr)
        return []
    return re.findall(r'href="([^"]+\.json)"', html)


def fetch_month(base, month):
    names = list_month(base, month)

    def one(name):
        for _ in range(3):
            try:
                return json.loads(read("%s%s%s/%s" % (base, REPO_PATH, month, name)))
            except Exception:
                pass
        return None

    with concurrent.futures.ThreadPoolExecutor(16) as pool:
        builds = [b for b in pool.map(one, names) if b is not None]
    print("  %s: %d builds" % (month, len(builds)), file=sys.stderr)
    return builds


def ended(build):
    return datetime.datetime.strptime(build["build_end_ts"], "%Y-%m-%dT%H:%M:%SZ")


def percentile(values, fraction):
    values = sorted(values)
    position = (len(values) - 1) * fraction
    low = int(position)
    high = min(low + 1, len(values) - 1)
    return values[low] + (values[high] - values[low]) * (position - low)


def clock(milliseconds):
    seconds = milliseconds / 1000.0
    return "%d:%02d" % (int(seconds // 60), int(round(seconds % 60)))


def measured(builds):
    """Builds whose three core stages all ran, so their durations are not truncated.

    A draft MR, a code-review-only run and a build that failed before a stage started all report a
    null duration for the stages that never ran; averaging those in understates every stage.
    """
    return [
        b for b in builds
        if not b.get("skipped")
        and not b.get("code_review_only")
        and all(b.get("stages", {}).get(k) is not None for k in CORE_STAGE_KEYS)
    ]


def print_distribution(builds):
    print("| field | n | median | p90 | p99 | mean |")
    fields = [("queue_wait_ms", lambda b: b["queue_wait_ms"]),
              ("duration_ms", lambda b: b["duration_ms"])]
    fields += [(key, lambda b, key=key: b["stages"].get(key)) for key in STAGE_KEYS]
    for name, value_of in fields:
        values = [v for v in (value_of(b) for b in builds) if v is not None]
        if not values:
            continue
        print("| %s | %d | %s | %s | %s | %s |" % (
            name, len(values), clock(statistics.median(values)), clock(percentile(values, 0.9)),
            clock(percentile(values, 0.99)), clock(statistics.mean(values))))


def print_critical_path(builds):
    longest = {}
    removable = []
    for build in builds:
        stages = {k: v for k, v in build["stages"].items() if v is not None}
        slowest = max(stages, key=stages.get)
        longest[slowest] = longest.get(slowest, 0) + 1
        others = [v for k, v in stages.items() if k != "unit_test_ms"]
        removable.append(max(0, stages["unit_test_ms"] - max(others)))

    total = len(builds)
    print("longest stage: " + ", ".join(
        "%s %.0f%%" % (k, 100.0 * c / total)
        for k, c in sorted(longest.items(), key=lambda kv: -kv[1])))
    shares = [b["stages"]["unit_test_ms"] / b["duration_ms"] for b in builds]
    print("unit_test_ms / duration_ms: median %.1f%%, mean %.1f%% "
          "(stages overlap, so this is not a share of wall-clock)"
          % (100 * statistics.median(shares), 100 * statistics.mean(shares)))
    print("wall-clock removable if the unit-test stage were free: "
          "median %s, p75 %s, p90 %s, mean %s"
          % (clock(statistics.median(removable)), clock(percentile(removable, 0.75)),
             clock(percentile(removable, 0.9)), clock(statistics.mean(removable))))
    print("  aggregate: %.1f%% of all pipeline wall-clock in the window"
          % (100.0 * sum(removable) / sum(b["duration_ms"] for b in builds)))

    for factor, label in [(0.5, "halved"), (0.75, "cut by 25%"), (0.9, "cut by 10%")]:
        saved = []
        for build in builds:
            stages = {k: v for k, v in build["stages"].items() if v is not None}
            before = max(stages.values())
            stages["unit_test_ms"] = stages["unit_test_ms"] * factor
            saved.append(before - max(stages.values()))
        print("  unit-test stage %s: median %s, mean %s saved"
              % (label, clock(statistics.median(saved)), clock(statistics.mean(saved))))

    agent_ms = sum(sum(v for v in b["stages"].values() if v) for b in builds)
    unit_ms = sum(b["stages"]["unit_test_ms"] for b in builds)
    print("agent-time: %.0f h across all stages, %.0f h in the unit-test stage (%.1f%%)"
          % (agent_ms / 3.6e6, unit_ms / 3.6e6, 100.0 * unit_ms / agent_ms))


def default_months():
    today = datetime.date.today()
    first = today.replace(day=1)
    previous = first - datetime.timedelta(days=1)
    return [previous.strftime("%Y/%m"), today.strftime("%Y/%m")]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--months", nargs="+", metavar="YYYY/MM", default=default_months())
    parser.add_argument("--days", type=int, help="keep only the last N days of the window")
    parser.add_argument("--json", metavar="FILE", help="write the raw builds to this file")
    args = parser.parse_args()

    base = base_url()
    print("reading %s%s" % (base, REPO_PATH), file=sys.stderr)
    builds = []
    for month in args.months:
        builds += fetch_month(base, month)
    if not builds:
        sys.exit("no build stats found for %s" % ", ".join(args.months))

    if args.json:
        with open(args.json, "w") as out:
            json.dump(builds, out)

    if args.days:
        cutoff = max(ended(b) for b in builds) - datetime.timedelta(days=args.days)
        builds = [b for b in builds if ended(b) >= cutoff]

    usable = measured(builds)
    successful = [b for b in usable if b.get("status") == "success"]
    print("\nwindow %s to %s" % (min(ended(b) for b in builds).date(), max(ended(b) for b in builds).date()))
    print("%d builds uploaded, %d with all core stages, %d of those successful"
          % (len(builds), len(usable), len(successful)))

    print("\n== successful builds ==")
    print_distribution(successful)
    print()
    print_critical_path(successful)

    failed = [b for b in usable if b.get("status") == "failure"]
    if failed:
        print("\n== failed builds (stages can be cut short by failStage/failFast) ==")
        print_distribution(failed)


if __name__ == "__main__":
    main()
