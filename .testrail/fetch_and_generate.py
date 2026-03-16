#!/usr/bin/env python3
"""
Fetches the latest ALPHA release test run results from TestRail for the current
app version, stores them as JSON in .testrail/results/, and runs
generate_slack_update.py to produce the Slack message.

Required environment variables:
  TESTRAIL_BASE_URL  – e.g. https://testrail.example.com
  TESTRAIL_USER      – TestRail email / username
  TESTRAIL_PASS      – TestRail API key or password
"""

import datetime
import json
import os
import re
import shutil
import subprocess
import sys
import traceback

import requests

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = os.path.join(SCRIPT_DIR, "results")
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "output")
ROOT_DIR = os.path.dirname(SCRIPT_DIR)
BUILD_GRADLE = os.path.join(ROOT_DIR, "build.gradle.kts")
GENERATE_SCRIPT = os.path.join(SCRIPT_DIR, "generate_slack_update.py")

ANDROID_PROJECT_ID = 5
MAX_AGE_DAYS = 14

STATUS_MAP = {
    1: "Passed",
    2: "Blocked",
    3: "Untested",
    4: "Retest",
    5: "Failed",
    6: "Parked",
    7: "Skipped",
    8: "Fixed",
    9: "Future Dev",
    10: "Feedback",
}


def get_app_version():
    """Extract appVersion from build.gradle.kts."""
    with open(BUILD_GRADLE, "r") as f:
        for line in f:
            match = re.search(r'extra\["appVersion"\]\s*=\s*"([^"]+)"', line)
            if match:
                return match.group(1)
    raise ValueError("Could not find appVersion in build.gradle.kts")


def testrail_get(session, base_url, endpoint, params=None):
    """GET from the TestRail API, handling pagination transparently."""
    url = f"{base_url}/index.php?/api/v2/{endpoint}"
    all_items = []
    offset = 0
    limit = 250

    while True:
        p = dict(params or {})
        p["limit"] = limit
        p["offset"] = offset
        resp = session.get(url, params=p)
        resp.raise_for_status()
        data = resp.json()

        # Paginated responses wrap items in a keyed dict with _links/size/etc.
        if isinstance(data, dict):
            # Find the payload key (runs, tests, results, etc.)
            payload_key = None
            for key in data:
                if key not in ("offset", "limit", "size", "_links"):
                    payload_key = key
                    break
            if payload_key and isinstance(data[payload_key], list):
                all_items.extend(data[payload_key])
                if data.get("size", 0) < limit:
                    break
                offset += limit
                continue
            # Non-paginated dict response (e.g. single object)
            return data
        elif isinstance(data, list):
            return data
        else:
            return data

    return all_items


def find_alpha_run(session, base_url, app_version):
    """Find the latest ALPHA test run for the given version within MAX_AGE_DAYS."""
    cutoff = datetime.datetime.now() - datetime.timedelta(days=MAX_AGE_DAYS)
    cutoff_ts = int(cutoff.timestamp())

    runs = testrail_get(
        session, base_url,
        f"get_runs/{ANDROID_PROJECT_ID}",
        params={"created_after": cutoff_ts},
    )

    alpha_runs = []
    for run in runs:
        name = (run.get("name") or "").lower()
        if f"v{app_version}" in name and "alpha" in name:
            alpha_runs.append(run)

    if not alpha_runs:
        raise RuntimeError(
            f"No ALPHA test run found for v{app_version} "
            f"within the last {MAX_AGE_DAYS} days"
        )

    return max(alpha_runs, key=lambda r: r["created_on"])


def fetch_tests(session, base_url, run_id):
    """Fetch all tests for a given run."""
    return testrail_get(session, base_url, f"get_tests/{run_id}")


def fetch_results_for_run(session, base_url, run_id):
    """Fetch all results for a run, grouped by test_id."""
    results = testrail_get(
        session, base_url,
        f"get_results_for_run/{run_id}",
    )
    grouped = {}
    for r in results:
        tid = r.get("test_id")
        if tid not in grouped:
            grouped[tid] = []
        grouped[tid].append(r)
    return grouped


def fetch_project_users(session, base_url):
    """Fetch users for the Android project and return a dict of id -> name."""
    users = testrail_get(session, base_url, f"get_users/{ANDROID_PROJECT_ID}")
    return {u["id"]: u.get("name", "") for u in users}


def build_json(tests, results_by_test, user_map):
    """Build a JSON structure for generate_slack_update.py to consume."""
    output_tests = []

    for t in tests:
        # Collect comments from all results for this test
        comments = []
        for r in results_by_test.get(t["id"], []):
            comment = r.get("comment")
            if comment:
                comments.append(comment)

        assignee_id = t.get("assignedto_id")
        assignee_name = user_map.get(assignee_id, "") if assignee_id else ""

        output_tests.append({
            "id": t["id"],
            "title": t.get("title", ""),
            "status": STATUS_MAP.get(t.get("status_id"), ""),
            "assignedto": assignee_name,
            "comments": comments,
        })

    return {"tests": output_tests}


def main():
    base_url = os.environ.get("TESTRAIL_BASE_URL", "").rstrip("/")
    user = os.environ.get("TESTRAIL_USER")
    password = os.environ.get("TESTRAIL_PASS")

    if not all([base_url, user, password]):
        raise EnvironmentError(
            "TESTRAIL_BASE_URL, TESTRAIL_USER, and TESTRAIL_PASS "
            "environment variables must be set"
        )

    session = requests.Session()
    session.auth = (user, password)
    session.headers["Content-Type"] = "application/json"

    # 1. Get app version from build.gradle.kts
    app_version = get_app_version()
    print(f"App version: {app_version}")

    # 2. Find the latest ALPHA run
    run = find_alpha_run(session, base_url, app_version)
    print(f"Found run: {run['name']} (ID: {run['id']})")

    # 3. Fetch tests and results
    tests = fetch_tests(session, base_url, run["id"])
    print(f"Fetched {len(tests)} tests")

    results_by_test = fetch_results_for_run(session, base_url, run["id"])
    print(f"Fetched results for {len(results_by_test)} tests")

    user_map = fetch_project_users(session, base_url)
    print(f"Fetched {len(user_map)} project users")
    print(f"  User IDs in map: {sorted(user_map.keys())}")

    # Log assignee IDs from tests and flag any missing from the user map
    assignee_ids = {t.get("assignedto_id") for t in tests} - {None}
    print(f"  Unique assignee IDs in tests: {sorted(assignee_ids)}")
    missing_ids = assignee_ids - set(user_map.keys())
    if missing_ids:
        print(f"  WARNING: assignee IDs not found in project users: {sorted(missing_ids)}")

    # 4. Build JSON
    data = build_json(tests, results_by_test, user_map)

    # 5. Clear results dir and write JSON
    if os.path.exists(RESULTS_DIR):
        shutil.rmtree(RESULTS_DIR)
    os.makedirs(RESULTS_DIR)

    json_filename = f"alpha_v{app_version}_run{run['id']}.json"
    json_path = os.path.join(RESULTS_DIR, json_filename)

    with open(json_path, "w") as f:
        json.dump(data, f, indent=2)
    print(f"Written: {json_path}")

    # 6. Run generate_slack_update.py
    print("Running generate_slack_update.py...")
    result = subprocess.run([sys.executable, GENERATE_SCRIPT])
    sys.exit(result.returncode)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        error_file = os.path.join(
            OUTPUT_DIR,
            f"errors{datetime.date.today().isoformat()}.txt",
        )
        with open(error_file, "w") as f:
            traceback.print_exc(file=f)
        traceback.print_exc()
        print(f"\nError details written to: {error_file}", file=sys.stderr)
        sys.exit(1)
