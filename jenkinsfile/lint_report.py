'''
This script analyses the Lint report(XML format) and prints a test summary.
Example output: 
{'Error': 248, 'Fatal': 17, 'Warning': 4781, 'Information': 1}

Usage:
This command must have 1 parameter of XML format lint report
    python3 junit_repor.py ${WORKSPACE}/${MODULE}build/report/lint-results.xml
'''

import sys
import os
import xml.etree.ElementTree as ET    
import xml.etree.ElementTree

if len(sys.argv) < 2:
    print("Parameter missing. Lint Report path is needed!")
    exit(0)

lintReportFile = sys.argv[1]
if not os.path.exists(lintReportFile):
    print("Internal error, Lint Report file does not exist")
    exit(0)

result = {}
try:
    tree = ET.parse(lintReportFile)
    issue_list = tree.getroot()
    for issue in issue_list:
        severity = issue.attrib['severity']
        if severity in result:
            result[severity] += 1
        else:
            result[severity] = 1
    output = ""
    for k, v in result.items():
        output = output + k + "(" + str(v) + ") "
    print(output)
except xml.etree.ElementTree.ParseError:
    print("Invalid format in Lint XML report!!")
except Exception as e:
    print("Unexpected error while parsing Lint report", e)

