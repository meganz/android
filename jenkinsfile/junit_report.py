'''
This script analyses the JUnit test report(XML format) and prints a
test summary.

The entire output only prints the values separated with a comma, in order to easily parse each value in a table.
The String is outputted as:
"[Total Cases],[Skipped],[Errors],[Failure],[Duration]"

Usage:
This command must have 1 parameter of root folder of test report, for example '${WORKSPACE}/app/build/test-results/testGmsDebugUnitTest'
    python3 junit_report.py ${WORKSPACE}/app/build/test-results/testGmsDebugUnitTest
'''

from bs4 import BeautifulSoup
import os
import sys


def parse_single_xml(file_name): 
    with open(file_name, 'r') as f:
        data  = f.read()

    bs_data = BeautifulSoup(data, 'xml')

    test_tag = bs_data.find_all('testsuite')[0]
    total = test_tag.get('tests')
    skipped = test_tag.get('skipped')
    failures = test_tag.get('failures')
    errors = test_tag.get('errors')
    duration = test_tag.get('time')

    return (total, skipped, failures, errors, duration)
    

if len(sys.argv) < 2:
    print("Parameter missing. Unit test report path is needed!")
    exit(0)

testReportRoot = sys.argv[1]
if not os.path.exists(testReportRoot):
    print("Internal error, test report folder does not exist")
    exit(0)

total_cases = 0
total_skipped = 0
total_failures = 0
total_errors = 0
total_duration = 0
rate = 0


for file in os.listdir(testReportRoot):
    if file.startswith("TEST-") and file.endswith(".xml"):
        values = parse_single_xml(os.path.join(testReportRoot, file))
        total_cases = total_cases + int(values[0])
        total_skipped = total_skipped + int(values[1])
        total_failures = total_failures + int(values[2])
        total_errors = total_errors + int(values[3])
        total_duration = total_duration + float(values[4])
if total_cases == 0:
    print("No Unit Test data available.")
    exit(0)
else:
    rate = "{0:.2%}".format((total_cases - total_failures - total_errors)/total_cases)

# In order to easily parse the results in a table format, print them without any label and separate each result with a comma
# The final results can be visualized as a String:
# "[Total Cases],[Skipped],[Errors],[Failure],[Duration]"
print("%s,%s,%s,%s,%s" % (str(total_cases), str(total_skipped), str(total_errors), str(total_failures), '{0:.3g}'.format(total_duration)))

