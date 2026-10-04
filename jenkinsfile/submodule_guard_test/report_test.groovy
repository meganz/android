/**
 * Checks how common.groovy lifts the explanation out of a failed checkSubmoduleChanges run.
 *
 * The MR comment the Build APK stage posts on a pointer move is built from that explanation. If
 * the parser misses it, the comment says the check could not reach a verdict, which reads like a
 * CI problem instead of the pointer move it is. The fixture is the real tail of the build that
 * tripped the guard on !17551, noise lines kept.
 *
 * Run from the repository root:
 *
 *     groovy -cp jenkinsfile/affected_modules_test/stubs \\
 *         jenkinsfile/submodule_guard_test/report_test.groovy
 */
def repoRoot = new File(".").absoluteFile.parentFile

def common = new GroovyShell().evaluate(new File(repoRoot, "jenkinsfile/common.groovy"))
int failed = 0
def check = { String name, boolean ok, Object detail = null ->
    if (ok) { println "PASS  ${name}" } else { failed++; println "FAIL  ${name}  ${detail}" }
}

String reason = common.gradleFailureReason(new File(repoRoot, "jenkinsfile/submodule_guard_test/fixtures/check_submodule_changes_output.txt").text)
List<String> reasonLines = reason.readLines()

check("reason opens with the verdict", reasonLines.first() == "This merge request moves an SDK submodule pointer:", reasonLines.first())
check("reason names the pointer", reasonLines[1] == "  sdk/src/main/jni/mega/sdk", reasonLines[1])
check("reason ends with the remedy", reasonLines.last() == "Restore the pointer, or move the change onto a release-flow branch.", reasonLines.last())
check("reason keeps the allowlist", reason.contains("- its branch is 'task/pre-release/v*', or"), reason)
check("gradle's task line is dropped", !reason.contains("Execution failed for task"), reason)
check("gradle's quoting is dropped", reasonLines.every { !it.startsWith("> ") }, reasonLines)
check("gradle's suggestions are dropped", !reason.contains("--stacktrace"), reason)
check("gradle's banner is dropped", !reason.contains("FAILURE: Build failed"), reason)
check("task output before the block is dropped", !reason.contains("Guarding submodule pointers"), reason)

check("no block gives nothing", common.gradleFailureReason("BUILD FAILED in 6s\nsome other noise") == "")
check("null output gives nothing", common.gradleFailureReason(null) == "")
check("empty output gives nothing", common.gradleFailureReason("") == "")

// A block Gradle never closed (output cut off mid-write) still yields what was captured.
String truncated = "* What went wrong:\nExecution failed for task ':x'.\n> Half a message"
check("unterminated block still yields its text", common.gradleFailureReason(truncated) == "Half a message", common.gradleFailureReason(truncated))

if (failed > 0) {
    println "${failed} check(s) failed"
    System.exit(1)
}
println "all checks passed"
