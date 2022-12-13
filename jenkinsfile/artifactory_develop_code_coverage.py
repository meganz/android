"""This Python script reads the text file (downloaded from Artifactory) containing the Code Coverage
results of the latest develop branch, and returns a String-formatted JSON Array.

The script requires 1 parameter that contains the full path of the text file:
python3 artifactory_develop_code_coverage.py ${WORKSPACE}/cicd/coverage/develop_coverage_summary.txt

Here is a sample output of the script:

[
   {
      "name": "**app**",
      "coveredLines": "3438",
      "totalLines": "178765",
      "totalTestCases": "504",
      "skippedTestCases": "2",
      "errorTestCases": "0",
      "failedTestCases": "0",
      "duration": "122"
   },
   {
      "name": "**domain**",
      "coveredLines": "834",
      "totalLines": "1096",
      "totalTestCases": "238",
      "skippedTestCases": "0",
      "errorTestCases": "0",
      "failedTestCases": "0",
      "duration": "1.57"
   }
]

Each JSON Array entry is a String-formatted Dictionary containing the following keys and their
purpose:

"name" - The module name
"coveredLines" - The number of lines in the module covered by tests
"totalLines" - The total number of lines in the module
"totalTestCases" - The total number of test cases in the module
"skippedTestCases" - The number of skipped test cases in the module
"errorTestCases" - The number of test cases returning an error in the module
"failedTestCases" - The number of failed test cases in the module
"duration" - Specifies how long the tests were executed in the module, measured in seconds (s)
"""

import json
import os
import sys

if len(sys.argv) < 2:
    print("Missing filepath of the Code Coverage results from the latest develop branch")
    exit(0)

artifactory_develop_file = sys.argv[1]
if not os.path.exists(artifactory_develop_file):
    print("File containing the Code Coverage of the latest develop branch does not exist")
    exit(0)

info_list = []

# Open the text file from the specified file path
file = open(artifactory_develop_file, "r")
# Iterate through all the lines in the text file
for line in file:

    """Here is a sample line in the text file:
    | **app** | 3438 | 178765 | 504 | 2 | 0 | 0 | 122 |
    
    Procedure to format each line, and adding it in info_list:
    
    1.) Remove the leading and trailing "|" through strip("|"). The output becomes:
      **app** | 3438 | 178765 | 504 | 2 | 0 | 0 | 122
      
    2.) Replace all "|" instances with "," through replace("|", ","). The output becomes:
      **app** , 3438 , 178765 , 504 , 2 , 0 , 0 , 122
      
    3.) Remove all whitespaces through replace(" ", ""). The output becomes:
    **app**,3438,178765,504,2,0,0,122
    
    4.) Convert the line into an array through split(","). The output becomes:
    ['**app**', '3438', '178765', '504', '2', '0', '0', '122']
    
    5.) Finally, create a Dictionary through line_dict.
    """
    line_array = line.strip("|").replace("|", ",").replace(" ", "").split(",")
    line_dict = {
        "name": line_array[0],
        "coveredLines": line_array[1],
        "totalLines": line_array[2],
        "totalTestCases": line_array[3],
        "skippedTestCases": line_array[4],
        "errorTestCases": line_array[5],
        "failedTestCases": line_array[6],
        "duration": line_array[7]
    }
    info_list.append(line_dict)
# Close the file after iterating through all available lines
file.close()

# Finally, return the String-formatted JSON Array
print(json.dumps(info_list, indent = 4))


