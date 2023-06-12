'''
This script generates a json file containing all the build warnings from a specified log output file

Usage:
This script required two parameters, a build log path and a target file path
'''

import json
import os
import re
import sys


def create_model(matches):
    type = "warning" if matches.group(1) == "w" else "error"
    issue_dictionary = {
        "type" : type,
        "file" : matches.group(2),
        "lineNumber" : matches.group(3),
        "issue" : matches.group(4),
    }
    return issue_dictionary


if len(sys.argv) < 3:
    print("Incorrect number of parameters. Please use: python3 warning_report.py <gradle log file path> <target file path>")
    exit(0)

gradle_log_file = sys.argv[1]
output_file = sys.argv[2]

if not os.path.exists(gradle_log_file):
    print("Specified log file does not exist. Please ensure path is correct.")
    exit(0)

pattern = re.compile("(w|e):.*\/([^:]+):(\d+):.*?\'(.*)$")
log = open(gradle_log_file)
log_lines = log.readlines()
count = 0
issue_list = []

for line in log_lines:
    for match in pattern.finditer(line):
        if match is not None:
            count += 1
            issue_list.append(create_model(match))


json_data = {
    "count": count,
    "issues": issue_list
}

with open(output_file, "w") as outfile:
    json.dump(json_data, outfile)