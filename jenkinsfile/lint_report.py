"""
Given a Module, this Python script analyzes the XML formatted Lint Report, and returns a
String of the Module's Lint Results, formatted as a JSON Object.

This command must have 1 parameter of the XML format lint report
    python3 lint_report.py ${WORKSPACE}/${MODULE}build/report/lint-results.xml

There are five Dictionary keys: 'fatalCount', 'errorCount', 'warningCount', 'informationCount'
and 'errorMessage'

Keys ending with "count" represent how many times a specific severity occurred. The four severities
are Fatal, Error, Warning or Information

The 'errorMessage' key holds the Error Message if something went wrong during script execution. If
there are no issues,'errorMessage' is simply empty

Here is a sample output of the String returned by the script, formatted as a JSON Object:

{"fatalCount": 10, "errorCount": 20, "warningCount": 30, "informationCount": 40, "errorMessage": ""}

It is important to call buildAndPrintDictionaryString() when printing the results or when an Exception
occurs, so that the results are properly rendered in Jenkins
"""

import json
import sys
import os
import xml.etree.ElementTree as ET    
import xml.etree.ElementTree

# Function that creates a Dictionary with the necessary parameters, then converts the Dictionary
# into a JSON Object-formatted String to be printed
def buildAndPrintDictionaryString(fatalCount, errorCount, warningCount, informationCount, errorMessage):
    issueDict = {
        'fatalCount': fatalCount,
        'errorCount': errorCount,
        'warningCount': warningCount,
        'informationCount': informationCount,
        'errorMessage': errorMessage
    }

    # Dump issueDict into a String and print the value
    print(json.dumps(issueDict))

if len(sys.argv) < 2:
    buildAndPrintDictionaryString(0, 0, 0, 0, "Parameter missing. Lint Report path is needed")
    exit(0)

lintReportFile = sys.argv[1]
if not os.path.exists(lintReportFile):
    buildAndPrintDictionaryString(0, 0, 0, 0, "Internal error, Lint Report file does not exist")
    exit(0)

try:
    # Declare four variables that will hold the overall count of a severity:
    fatalCount = 0
    errorCount = 0
    warningCount = 0
    informationCount = 0

    tree = ET.parse(lintReportFile)
    issue_list = tree.getroot()
    for issue in issue_list:
        # Retrieve the Severity Type
        severity = issue.attrib['severity']
        # Fatal Severity
        if severity == 'Fatal':
            fatalCount += 1
        # Error Severity
        elif severity == 'Error':
            errorCount += 1
        # Warning Severity
        elif severity == 'Warning':
            warningCount += 1
        # Information Severity
        elif severity == 'Information':
            informationCount += 1

    buildAndPrintDictionaryString(str(fatalCount), str(errorCount), str(warningCount), str(informationCount), "None")
except xml.etree.ElementTree.ParseError:
    buildAndPrintDictionaryString(0, 0, 0, 0, "Invalid format in Lint XML report")
except Exception as e:
    buildAndPrintDictionaryString(0, 0, 0, 0, "Unexpected error while parsing Lint report: {}".format(e))

