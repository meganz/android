/**
 * Checks how common.groovy reads the marker lines that printAffectedModules writes to stdout.
 *
 * Recognising those markers is the part of shadow mode that can fail silently: output the parser
 * does not understand looks exactly like a merge request that affects nothing, which is the one
 * reading that must never be assumed. The fixture is trimmed real task output, noise lines kept.
 *
 * Also covers the branch-name handling that made the first shadow runs report nothing at all.
 *
 * Run from the repository root:
 *
 *     groovy -cp jenkinsfile/affected_modules_test/stubs \\
 *         jenkinsfile/affected_modules_test/parser_test.groovy
 *
 * The stub supplies the one Jenkins class common.groovy names, so it compiles outside Jenkins.
 */
def repoRoot = new File(".").absoluteFile.parentFile

def common = new GroovyShell().evaluate(new File(repoRoot, "jenkinsfile/common.groovy"))
int failed = 0
def check = { String name, boolean ok, Object detail = null ->
    if (ok) { println "PASS  ${name}" } else { failed++; println "FAIL  ${name}  ${detail}" }
}

def real = common.parseAffectedModulesOutput(new File(repoRoot, "jenkinsfile/affected_modules_test/fixtures/print_affected_modules_output.txt").text)
check("real output is available", real.available)
check("real output mode parsed", real.mode == "selected (2 of 68 modules)", real.mode)
check("real output modules parsed", real.modules == ["app", "feature/share-link"], real.modules)

def full = common.parseAffectedModulesOutput("""
noise line that should be ignored
AFFECTED-MODULES-MODE: full (67 modules) - always-full path changed: gradle/catalogs/lib.versions.toml
AFFECTED-MODULE: app
AFFECTED-MODULE: domain
""")
check("full run available", full.available)
check("full run reason kept", full.mode.contains("always-full path changed"), full.mode)
check("full run modules", full.modules == ["app", "domain"], full.modules)

def empty = common.parseAffectedModulesOutput("AFFECTED-MODULES-MODE: selected (0 of 68 modules)")
check("selected nothing is still available", empty.available && empty.modules.isEmpty())

// Every unavailable result has to say why: without a reason, a permanently broken detector and a
// quiet one produce identical reports, which is exactly what happened on the first shadow builds.
check("available result carries no reason", real.reason == "", real.reason)

def silent = common.parseAffectedModulesOutput("some gradle noise\nand nothing else")
check("missing mode line explains itself", silent.reason?.contains("AFFECTED-MODULES-MODE"), silent.reason)
check("no mode line is unavailable", !silent.available && silent.modules.isEmpty())
check("null output is unavailable", !common.parseAffectedModulesOutput(null).available)
check("empty output is unavailable", !common.parseAffectedModulesOutput("").available)

check("plain branch accepted", common.isSafeBranchName("do/AND-1-x"))
check("option-like branch rejected", !common.isSafeBranchName("--upload-pack=evil"))
check("branch with space rejected", !common.isSafeBranchName("dev elop"))
check("branch with semicolon rejected", !common.isSafeBranchName("develop; echo pwned"))
check("branch with backtick rejected", !common.isSafeBranchName('develop`whoami`'))
check("null branch rejected", !common.isSafeBranchName(null))

// Jenkins hands GIT_BRANCH over with the remote on the front, and the detector prefixes origin/
// itself, so passing it through produced origin/origin/<branch> and git rejected the revision.
check("origin prefix stripped", common.bareBranchName("origin/do/AND-1-x") == "do/AND-1-x")
check("bare branch untouched", common.bareBranchName("do/AND-1-x") == "do/AND-1-x")
check("only the leading origin goes", common.bareBranchName("origin/origin/x") == "origin/x")
check("origin inside a name is kept", common.bareBranchName("do/origin/x") == "do/origin/x")
check("whitespace trimmed", common.bareBranchName("  develop  ") == "develop")
check("null branch name survives", common.bareBranchName(null) == null)

// A Gradle failure runs to dozens of lines; the report keeps the first one.
check("first line taken", common.firstLineOf("boom\nstack\nmore") == "boom")
check("leading blank lines skipped", common.firstLineOf("\n\nreal message") == "real message")
check("null message handled", common.firstLineOf(null) == "unknown error")

// Jenkins reports a failed sh only as "script returned exit code 1", so the detector's own output
// is captured and its tail carried into the report - Gradle names the cause at the end.
def gradleFailure = """
> Task :printAffectedModules FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':printAffectedModules'.
> fatal: ambiguous argument 'origin/develop...origin/nope': unknown revision
"""
check("tail names the cause", common.tailOf(gradleFailure, 2).contains("unknown revision"),
      common.tailOf(gradleFailure, 2))
check("tail skips banner lines", !common.tailOf(gradleFailure, 2).contains("FAILURE: Build failed"))
check("tail joins lines readably", common.tailOf(gradleFailure, 2).contains(" | "))
check("tail of empty is empty", common.tailOf("", 4) == "")
check("tail of null is empty", common.tailOf(null, 4) == "")
check("tail is capped", common.tailOf("x" * 900, 4).length() <= 400)


// The report is what phase 5 aggregates, so it has to be machine-readable every time.
def json = common.affectedModuleShadowJson(
    "42", "17486",
    [available: true, mode: "selected (2 of 68 modules)"],
    ["app", "domain", "feature/share-link"],
    ["app", "feature/share-link"],
    ["domain"],
    ["domain"],
    ["domain"],
)
def parsed = new groovy.json.JsonSlurper().parseText(json)
check("report is valid json", parsed != null)
check("report records the miss", parsed.missed == ["domain"], parsed.missed)
check("report records what would run", parsed.wouldRun == ["app", "feature/share-link"], parsed.wouldRun)
check("report records counts", parsed.unitTestModuleCount == 3 && parsed.wouldSkipCount == 1, parsed)
check("report records detector availability", parsed.detectorAvailable == true)

def unavailableJson = common.affectedModuleShadowJson(
    "42", "17486",
    common.unavailableAffectedModules('detector failed: [Error] command failed: "git diff"'),
    ["app"], [], [], [], [],
)
def unavailableParsed = new groovy.json.JsonSlurper().parseText(unavailableJson)
check("unavailable detector is recorded as such", unavailableParsed.detectorAvailable == false)
check("unavailable detector reports no misses", unavailableParsed.missed == [])
check("unavailable reason is recorded", unavailableParsed.unavailableReason.contains("command failed"),
      unavailableParsed.unavailableReason)
check("quotes in the reason do not break the json",
      unavailableParsed.unavailableReason.contains('"git diff"'), unavailableParsed.unavailableReason)


// The comparison that produces the miss list. Written without intersect/minus because the Jenkins
// sandbox rejects those; these assertions pin the behaviour so a "tidier" rewrite cannot reinstate
// a method that only fails on a real agent.
def cmp = common.compareAffectedModules(
    ["app", "domain", "feature/chat/chat", "data"],
    ["app", "feature/chat/chat", "resources/icon-pack"],
    ["domain"],
)
check("selected keeps only unit-test modules the detector picked",
      cmp.selected == ["app", "feature/chat/chat"], cmp.selected)
check("detector modules without unit tests are ignored",
      !cmp.selected.contains("resources/icon-pack"), cmp.selected)
check("skipped is everything else", cmp.skipped == ["data", "domain"], cmp.skipped)
check("a failure in a skipped module is a miss", cmp.missed == ["domain"], cmp.missed)

def noMiss = common.compareAffectedModules(["app", "domain"], ["app", "domain"], ["domain"])
check("no miss when the failing module was selected", noMiss.missed == [], noMiss.missed)

def nothing = common.compareAffectedModules(["app"], [], [])
check("nothing selected means everything skipped", nothing.skipped == ["app"], nothing.skipped)
check("no failures means no misses", nothing.missed == [], nothing.missed)

def green = common.compareAffectedModules(["app", "domain"], ["app"], [])
check("a green build reports no misses", green.missed == [], green.missed)

println failed == 0 ? "\nALL PASSED" : "\n${failed} FAILED"
System.exit(failed)
