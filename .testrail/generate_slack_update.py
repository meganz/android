#!/usr/bin/env python3
"""
Parses the latest TestRail XML export from .testrail/results/ and generates a Slack
update file listing failed and feedback test cases grouped by assigned developer.
Automated tests (containing "failed by automated test") are excluded.

Output: .testrail/output/SlackUpdate<YYYY-MM-DD>.txt
"""

import glob
import os
import re
import sys
import traceback
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import date

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = os.path.join(SCRIPT_DIR, "results")
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "output")
TEST_VIEW_PATH = "index.php?/tests/view"


def find_latest_xml():
    xml_files = glob.glob(os.path.join(RESULTS_DIR, "*.xml"))
    if not xml_files:
        raise FileNotFoundError("No XML files found in .testrail/results/")
    return max(xml_files, key=os.path.getmtime)


def extract_version(filename):
    match = re.search(r"v(\d+\.\d+)", os.path.basename(filename))
    if not match:
        raise ValueError(f"Could not extract version from filename: {filename}")
    return f"v{match.group(1)}"


def is_automated(test):
    full_text = ET.tostring(test, encoding="unicode")
    cleaned = re.sub(r"\*+", "", full_text).lower()
    return "failed by automated test" in cleaned


def get_base_url():
    base = os.environ.get("TESTRAIL_BASE_URL")
    if not base:
        raise EnvironmentError("TESTRAIL_BASE_URL environment variable is not set")
    return f"{base.rstrip('/')}/{TEST_VIEW_PATH}"


def parse_tests(xml_path):
    base_url = get_base_url()
    tree = ET.parse(xml_path)
    root = tree.getroot()

    grouped = defaultdict(list)

    for test in root.iter("test"):
        status_el = test.find("status")
        if status_el is None or not status_el.text:
            continue
        status = status_el.text.strip().lower()
        if status not in ("failed", "feedback"):
            continue
        if is_automated(test):
            continue

        test_id_el = test.find("id")
        assignee_el = test.find("assignedto")

        tid = test_id_el.text.strip() if test_id_el is not None and test_id_el.text else "N/A"
        numeric_id = tid.lstrip("T")
        assignee = assignee_el.text.strip() if assignee_el is not None and assignee_el.text and assignee_el.text.strip() else "Unassigned"

        entry = f"• [{tid}]({base_url}/{numeric_id})"
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
    xml_path = find_latest_xml()
    version = extract_version(xml_path)
    grouped = parse_tests(xml_path)

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
