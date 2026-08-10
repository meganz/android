#!/usr/bin/env python3
"""
Parses the latest TestRail JSON results from .testrail/results/ and generates a Slack
update file listing failed and feedback test cases grouped by assigned developer.
Automated tests (containing "failed by automated test") are excluded.

Output: .testrail/output/SlackUpdate<YYYY-MM-DD>.txt
"""

import glob
import json
import os
import re
import sys
import traceback
from collections import defaultdict
from datetime import date

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = os.path.join(SCRIPT_DIR, "results")
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "output")
TEST_VIEW_PATH = "index.php?/tests/view"


def find_latest_json():
    json_files = glob.glob(os.path.join(RESULTS_DIR, "*.json"))
    if not json_files:
        raise FileNotFoundError("No JSON files found in .testrail/results/")
    return max(json_files, key=os.path.getmtime)


def extract_version(filename):
    match = re.search(r"v(\d+\.\d+)", os.path.basename(filename))
    if not match:
        raise ValueError(f"Could not extract version from filename: {filename}")
    return f"v{match.group(1)}"


def is_automated(test):
    """Check if any result comment indicates an automated test."""
    for comment in test.get("comments", []):
        cleaned = re.sub(r"\*+", "", comment).lower()
        if "failed by automated test" in cleaned:
            return True
    return False


def get_base_url():
    base = os.environ.get("TESTRAIL_BASE_URL")
    if not base:
        raise EnvironmentError("TESTRAIL_BASE_URL environment variable is not set")
    return f"{base.rstrip('/')}/{TEST_VIEW_PATH}"


def parse_tests(json_path):
    base_url = get_base_url()

    with open(json_path, "r") as f:
        data = json.load(f)

    grouped = defaultdict(list)

    for test in data.get("tests", []):
        status = (test.get("status") or "").strip().lower()
        if status not in ("failed", "feedback"):
            continue
        if is_automated(test):
            continue

        tid = test.get("id", "N/A")
        numeric_id = str(tid).lstrip("T")
        assignee = (test.get("assignedto") or "").strip() or "Unassigned"

        entry = f"• [T{numeric_id}]({base_url}/{numeric_id})"
        if status == "feedback":
            entry += " (Feedback)"

        grouped[assignee].append(entry)

    return grouped


def generate_output(grouped, version):
    header = (
        f"Hi Android team, a few TC has been marked as Failed  or Feedback  in latest "
        f"Android *{version}* release. Please address them as a priority in your workloads "
        f"and don't forget to update their status in TestRail (Fixed if you have cherry "
        f"picked a fix to the release branch, Retest  otherwise) once the fix has been completed."
    )

    lines = [header, ""]

    # Assigned developers first, then Unassigned at the end
    assigned = {k: v for k, v in grouped.items() if k != "Unassigned"}
    unassigned = grouped.get("Unassigned", [])

    for developer, entries in sorted(assigned.items()):
        lines.append(f"*{developer}*")
        lines.extend(entries)
        lines.append("")

    if unassigned:
        lines.append("*Unassigned*")
        lines.extend(unassigned)
        lines.append("")

    return "\n".join(lines)


def main():
    json_path = find_latest_json()
    version = extract_version(json_path)
    grouped = parse_tests(json_path)

    output = generate_output(grouped, version)
    output_file = os.path.join(
        OUTPUT_DIR,
        f"SlackUpdate{date.today().isoformat()}.txt",
    )

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    with open(output_file, "w") as f:
        f.write(output)

    print(f"Generated: {output_file}")
    print(f"Version: {version}")
    total = sum(len(v) for v in grouped.values())
    print(f"Total test cases: {total}")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        error_file = os.path.join(
            OUTPUT_DIR,
            f"errors{date.today().isoformat()}.txt",
        )
        with open(error_file, "w") as f:
            traceback.print_exc(file=f)
        print(f"Error details written to: {error_file}", file=sys.stderr)
        sys.exit(1)
