#!/usr/bin/env python3
"""
Renders the accessibility JSON reports produced by the :a11y-test module into a single,
self-contained HTML page aimed at designers.

The managed-device run (:a11y-test:ciPixelDebugAndroidTest) serializes every Accessibility
Test Framework check result (see AccessibilityCheckReporter in :a11y-test) to JSON files that
Gradle Managed Devices pulls to:
    a11y-test/build/outputs/managed_device_android_test_additional_output/debug/ciPixel/a11y/
Screenshots taken during the run may be pulled alongside the JSON files.

Each JSON file has the shape:
    {"screenshotPath": <string|null>,
     "results": [{"check": <simpleName>, "type": "ERROR|WARNING|INFO|SUPPRESSED|NOT_RUN",
                  "element": <string|null>, "message": <string|null>,
                  "metadata": {<KEY_CONTRAST_RATIO etc>: <value>}}]}

Usage:
    python3 jenkinsfile/a11y_report.py <input-dir> <output.html> [--summary <summary.txt>]

<input-dir> is scanned recursively for *.json report files and image files. The optional
summary file receives a short plain-text digest suitable for a Slack message.

Only the Python standard library is used. Fixture inputs for local verification live in
jenkinsfile/a11y_report_fixtures/.
"""

import argparse
import base64
import html
import json
import re
import sys
from collections import Counter, OrderedDict
from pathlib import Path

DESIGN_CHECKS = ("TextContrastCheck", "ImageContrastCheck", "TextSizeCheck")

CHECK_TITLES = {
    "TextContrastCheck": "Text contrast",
    "ImageContrastCheck": "Image and icon contrast",
    "TextSizeCheck": "Text size",
    "TouchTargetSizeCheck": "Touch target size",
}

CHECK_INTROS = {
    "TextContrastCheck": (
        "Text needs to stand out clearly from the background behind it, otherwise people "
        "with low vision or a sunlit screen cannot read it. Each row shows the measured "
        "contrast between the text colour and its background, and the minimum it should reach."
    ),
    "ImageContrastCheck": (
        "Icons and images that carry meaning need enough contrast against their background "
        "to stay visible. Each row shows the measured contrast for one image."
    ),
    "TextSizeCheck": (
        "Text defined with a fixed unit does not grow when a user increases the font size in "
        "their device settings. Each row shows the estimated size of the affected text."
    ),
    "TouchTargetSizeCheck": (
        "Buttons and other tappable elements need to be large enough to hit reliably. Each "
        "row shows the measured size of the tappable area and the minimum it should reach."
    ),
}

# Metadata keys that represent an actual measurement. SUPPRESSED entries carrying none of
# these are bookkeeping noise and are excluded from the report.
MEASURED_KEYS = {
    "KEY_CONTRAST_RATIO",
    "KEY_REQUIRED_CONTRAST_RATIO",
    "KEY_ESTIMATED_TEXT_SIZE_DP",
    "KEY_TEXT_SIZE_UNIT",
    "KEY_WIDTH",
    "KEY_HEIGHT",
    "KEY_REQUIRED_WIDTH",
    "KEY_REQUIRED_HEIGHT",
}

TEXT_SIZE_UNITS = {0: "px", 1: "dp", 2: "sp", 3: "pt", 4: "in", 5: "mm"}

IMAGE_MIME_TYPES = {
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".webp": "image/webp",
}

MAX_EMBEDDED_IMAGE_BYTES = 2 * 1024 * 1024


def check_title(check_name):
    if check_name in CHECK_TITLES:
        return CHECK_TITLES[check_name]
    words = re.sub(r"Check$", "", check_name)
    return re.sub(r"(?<!^)([A-Z])", r" \1", words).capitalize()


def screen_name(report_file):
    """Derives a screen name from the report file name.

    Report files are named `<reportName>-<timestampMillis>-<counter>.json`, where reportName
    is typically the test class that exercised the screen.
    """
    stem = re.sub(r"-\d{9,}-\d+$", "", report_file.stem)
    return re.sub(r"Test$", "", stem)


def element_label(element):
    if not element:
        return "(screen level)"
    resource = re.search(r"\S+:id/(\S+)", element)
    if resource:
        return resource.group(1)
    class_name = re.match(r"[\w.$]+", element)
    if class_name:
        return class_name.group(0).split(".")[-1].split("$")[-1]
    return element


def color_hex(value):
    try:
        return "#{:06X}".format(int(value) & 0xFFFFFF)
    except (TypeError, ValueError):
        return None


def format_number(value):
    try:
        number = float(value)
    except (TypeError, ValueError):
        return str(value)
    return "{:g}".format(round(number, 2))


def color_chip(label, value):
    hex_value = color_hex(value)
    if hex_value is None:
        return ""
    return (
        '<span class="chip"><span class="swatch" style="background:{0}"></span>'
        "{1} {0}</span>".format(hex_value, html.escape(label))
    )


def measurement_html(entry):
    """Returns the 'measured vs required' cell for one finding, as safe HTML."""
    metadata = entry["metadata"]
    check = entry["check"]
    parts = []
    if check in ("TextContrastCheck", "ImageContrastCheck"):
        ratio = metadata.get("KEY_CONTRAST_RATIO")
        required = metadata.get("KEY_REQUIRED_CONTRAST_RATIO")
        if ratio is not None:
            parts.append("Measured contrast <b>{}:1</b>".format(format_number(ratio)))
        if required is not None:
            parts.append("needs at least {}:1".format(format_number(required)))
        chips = "".join(
            color_chip(label, metadata.get(key))
            for label, key in (
                ("text", "KEY_TEXT_COLOR"),
                ("foreground", "KEY_FOREGROUND_COLOR"),
                ("background", "KEY_BACKGROUND_COLOR"),
            )
            if key in metadata
        )
        if chips:
            parts.append('<span class="chips">{}</span>'.format(chips))
    elif check == "TextSizeCheck":
        size = metadata.get("KEY_ESTIMATED_TEXT_SIZE_DP")
        unit = TEXT_SIZE_UNITS.get(metadata.get("KEY_TEXT_SIZE_UNIT"))
        if size is not None:
            parts.append("Estimated size <b>{} dp</b>".format(format_number(size)))
        if unit:
            parts.append("defined in {} (does not follow the font-size setting)".format(unit))
    elif check == "TouchTargetSizeCheck":
        width = metadata.get("KEY_WIDTH")
        height = metadata.get("KEY_HEIGHT")
        required_width = metadata.get("KEY_REQUIRED_WIDTH")
        required_height = metadata.get("KEY_REQUIRED_HEIGHT")
        if width is not None and height is not None:
            parts.append(
                "Tappable area <b>{} × {} dp</b>".format(
                    format_number(width), format_number(height)
                )
            )
        if required_width is not None and required_height is not None:
            parts.append(
                "needs at least {} × {} dp".format(
                    format_number(required_width), format_number(required_height)
                )
            )
    if not parts:
        parts = [
            "{} = {}".format(html.escape(key), html.escape(format_number(value)))
            for key, value in sorted(metadata.items())
            if key in MEASURED_KEYS
        ]
    return ", ".join(parts) if parts else "&#8212;"


def is_reportable(result):
    """Applies the noise filter: NOT_RUN and unmeasured SUPPRESSED entries are dropped."""
    result_type = result.get("type")
    if result_type == "NOT_RUN":
        return False
    if result_type == "SUPPRESSED":
        metadata = result.get("metadata") or {}
        return any(key in MEASURED_KEYS for key in metadata)
    return True


def load_screens(input_dir):
    """Parses every report JSON under input_dir, grouped by screen.

    Returns an ordered mapping of screen name to
    {"screenshot": Path|None, "entries": [finding, ...]} with noise filtered out and
    duplicate findings (the same result re-delivered by a later check run) collapsed.
    """
    images_by_name = {
        path.name: path
        for path in sorted(input_dir.rglob("*"))
        if path.suffix.lower() in IMAGE_MIME_TYPES
    }
    screens = OrderedDict()
    seen = set()
    for report_file in sorted(input_dir.rglob("*.json")):
        try:
            report = json.loads(report_file.read_text())
        except (OSError, ValueError) as error:
            print("Skipping unreadable report {}: {}".format(report_file, error))
            continue
        if not isinstance(report, dict) or not isinstance(report.get("results"), list):
            continue
        screen = screen_name(report_file)
        record = screens.setdefault(screen, {"screenshot": None, "entries": []})
        screenshot_path = report.get("screenshotPath")
        if screenshot_path and record["screenshot"] is None:
            record["screenshot"] = images_by_name.get(Path(screenshot_path).name)
        for result in report["results"]:
            if not isinstance(result, dict) or not is_reportable(result):
                continue
            entry = {
                "screen": screen,
                "check": str(result.get("check") or "UnknownCheck"),
                "type": str(result.get("type") or "UNKNOWN"),
                "element": result.get("element"),
                "message": result.get("message"),
                "metadata": result.get("metadata") or {},
            }
            key = json.dumps(entry, sort_keys=True)
            if key in seen:
                continue
            seen.add(key)
            record["entries"].append(entry)
    return screens


def embedded_image(path):
    if path is None:
        return None
    try:
        data = path.read_bytes()
    except OSError:
        return None
    if not data or len(data) > MAX_EMBEDDED_IMAGE_BYTES:
        return None
    mime = IMAGE_MIME_TYPES[path.suffix.lower()]
    return "data:{};base64,{}".format(mime, base64.b64encode(data).decode("ascii"))


def screen_anchor(screen):
    return "screen-" + re.sub(r"[^A-Za-z0-9]+", "-", screen).strip("-").lower()


def findings_table(entries, show_result_type=False):
    header_cells = ["Screen", "Component", "Measured vs required", "Details"]
    if show_result_type:
        header_cells.insert(2, "Severity")
    rows = []
    for entry in entries:
        cells = [
            '<a href="#{}">{}</a>'.format(
                screen_anchor(entry["screen"]), html.escape(entry["screen"])
            ),
            "<b>{}</b><br/><code>{}</code>".format(
                html.escape(element_label(entry["element"])),
                html.escape(entry["element"] or ""),
            ),
            measurement_html(entry),
            html.escape(entry["message"] or ""),
        ]
        if show_result_type:
            cells.insert(2, html.escape(entry["type"].capitalize()))
        rows.append("<tr>{}</tr>".format("".join("<td>{}</td>".format(c) for c in cells)))
    return (
        '<table><thead><tr>{}</tr></thead><tbody>{}</tbody></table>'.format(
            "".join("<th>{}</th>".format(html.escape(cell)) for cell in header_cells),
            "".join(rows),
        )
    )


STYLE = """
body { font-family: -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
       margin: 0 auto; max-width: 1080px; padding: 24px; color: #1c1c1c; background: #fff; }
h1 { font-size: 26px; margin-bottom: 4px; }
h2 { font-size: 20px; margin-top: 40px; border-bottom: 2px solid #e0e0e0; padding-bottom: 6px; }
h3 { font-size: 16px; margin-top: 28px; margin-bottom: 4px; }
p.intro { color: #555; margin-top: 4px; max-width: 860px; }
table { border-collapse: collapse; width: 100%; margin-top: 12px; font-size: 13px; }
th { text-align: left; background: #f4f4f4; }
th, td { border: 1px solid #ddd; padding: 8px 10px; vertical-align: top; }
code { font-size: 11px; color: #777; word-break: break-all; }
.summary { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 16px; }
.card { border: 1px solid #ddd; border-radius: 8px; padding: 12px 18px; min-width: 130px; }
.card .count { font-size: 26px; font-weight: 700; }
.card.error .count { color: #c62828; }
.card .label { color: #666; font-size: 12px; }
.screens { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 12px; }
.screen { border: 1px solid #ddd; border-radius: 8px; padding: 10px; width: 220px; }
.screen img { max-width: 100%; border: 1px solid #eee; }
.screen .name { font-weight: 600; margin-bottom: 6px; }
.screen .meta { color: #666; font-size: 12px; }
.chips { display: inline-block; }
.chip { white-space: nowrap; margin-right: 8px; font-size: 12px; color: #555; }
.swatch { display: inline-block; width: 12px; height: 12px; border: 1px solid #999;
          vertical-align: -1px; margin-right: 3px; }
.empty { color: #2e7d32; font-weight: 600; }
footer { margin-top: 48px; color: #999; font-size: 12px; }
"""


def render_html(screens):
    all_entries = [entry for record in screens.values() for entry in record["entries"]]
    gate_errors = [entry for entry in all_entries if entry["type"] == "ERROR"]
    design_findings = [
        entry
        for entry in all_entries
        if entry["check"] in DESIGN_CHECKS and entry["type"] != "ERROR"
    ]
    other_findings = [
        entry
        for entry in all_entries
        if entry not in gate_errors and entry not in design_findings
    ]

    check_counts = Counter(entry["check"] for entry in all_entries)
    type_counts = Counter(entry["type"] for entry in all_entries)

    out = ["<!DOCTYPE html>", '<html lang="en"><head><meta charset="utf-8"/>']
    out.append("<title>MEGA Android accessibility report</title>")
    out.append("<style>{}</style></head><body>".format(STYLE))
    out.append("<h1>MEGA Android accessibility report</h1>")
    out.append(
        '<p class="intro">Automated accessibility checks run against the app on an emulated '
        "Pixel device. The findings below are measurements, not judgements &#8212; some may "
        "be acceptable in context.</p>"
    )

    out.append("<h2>Summary</h2>")
    out.append('<div class="summary">')
    out.append(
        '<div class="card"><div class="count">{}</div>'
        '<div class="label">Screens checked</div></div>'.format(len(screens))
    )
    out.append(
        '<div class="card"><div class="count">{}</div>'
        '<div class="label">Findings for design review</div></div>'.format(
            len(design_findings)
        )
    )
    out.append(
        '<div class="card error"><div class="count">{}</div>'
        '<div class="label">Blocking issues (developer gate)</div></div>'.format(
            len(gate_errors)
        )
    )
    out.append("</div>")
    if all_entries:
        out.append("<h3>Findings by check</h3><table><thead><tr><th>Check</th>")
        out.append("<th>Findings</th></tr></thead><tbody>")
        for check, count in check_counts.most_common():
            out.append(
                "<tr><td>{}</td><td>{}</td></tr>".format(
                    html.escape(check_title(check)), count
                )
            )
        out.append("</tbody></table>")
        out.append("<h3>Findings by result</h3><table><thead><tr><th>Result</th>")
        out.append("<th>Findings</th></tr></thead><tbody>")
        for result_type, count in type_counts.most_common():
            out.append(
                "<tr><td>{}</td><td>{}</td></tr>".format(
                    html.escape(result_type.capitalize()), count
                )
            )
        out.append("</tbody></table>")

    out.append("<h2>Screens checked</h2>")
    if screens:
        out.append('<div class="screens">')
        for screen, record in screens.items():
            out.append('<div class="screen" id="{}">'.format(screen_anchor(screen)))
            out.append('<div class="name">{}</div>'.format(html.escape(screen)))
            image = embedded_image(record["screenshot"])
            if image:
                out.append('<img src="{}" alt="{}"/>'.format(image, html.escape(screen)))
            else:
                out.append('<div class="meta">No screenshot captured</div>')
            out.append(
                '<div class="meta">{} finding(s)</div>'.format(len(record["entries"]))
            )
            out.append("</div>")
        out.append("</div>")
    else:
        out.append("<p>No accessibility reports were found for this run.</p>")

    out.append("<h2>For design review</h2>")
    out.append(
        '<p class="intro">These findings relate to colour, contrast and text size, so the '
        "design team owns the call on each one. They do not block builds.</p>"
    )
    if design_findings:
        for check in DESIGN_CHECKS:
            entries = [entry for entry in design_findings if entry["check"] == check]
            if not entries:
                continue
            out.append("<h3>{}</h3>".format(html.escape(check_title(check))))
            out.append('<p class="intro">{}</p>'.format(html.escape(CHECK_INTROS[check])))
            out.append(findings_table(entries))
    else:
        out.append('<p class="empty">No design-related findings in this run.</p>')

    out.append("<h2>Blocking issues</h2>")
    out.append(
        '<p class="intro">These findings fail the automated developer gate and are fixed in '
        "code; they are listed here for visibility.</p>"
    )
    if gate_errors:
        out.append(findings_table(gate_errors))
    else:
        out.append('<p class="empty">No blocking issues in this run.</p>')

    if other_findings:
        out.append("<h2>Other observations</h2>")
        out.append(
            '<p class="intro">Lower-severity results that neither block builds nor need a '
            "design decision.</p>"
        )
        out.append(findings_table(other_findings, show_result_type=True))

    out.append(
        "<footer>Generated by jenkinsfile/a11y_report.py from the "
        ":a11y-test:ciPixelDebugAndroidTest run.</footer>"
    )
    out.append("</body></html>")

    summary_lines = [
        "Screens checked: {}".format(len(screens)),
        "Findings for design review: {}{}".format(
            len(design_findings),
            " ({})".format(
                ", ".join(
                    "{} {}".format(
                        check_title(check),
                        sum(1 for e in design_findings if e["check"] == check),
                    )
                    for check in DESIGN_CHECKS
                    if any(e["check"] == check for e in design_findings)
                )
            )
            if design_findings
            else "",
        ),
        "Blocking issues (developer gate): {}".format(len(gate_errors)),
    ]
    if other_findings:
        summary_lines.append("Other observations: {}".format(len(other_findings)))
    return "\n".join(out), "\n".join(summary_lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_dir", help="directory holding the a11y JSON reports")
    parser.add_argument("output", help="path of the HTML report to write")
    parser.add_argument("--summary", help="optional path for a plain-text Slack summary")
    args = parser.parse_args()

    input_dir = Path(args.input_dir)
    if not input_dir.is_dir():
        print("Input directory does not exist: {}".format(input_dir))
        return 2

    screens = load_screens(input_dir)
    page, summary = render_html(screens)
    Path(args.output).write_text(page)
    print("Wrote {} ({} screens, {} findings)".format(
        args.output, len(screens), sum(len(r["entries"]) for r in screens.values())
    ))
    if args.summary:
        Path(args.summary).write_text(summary + "\n")
    print(summary)
    return 0


if __name__ == "__main__":
    sys.exit(main())
