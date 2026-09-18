import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps the affected-module configuration in the root build file honest against the repository
 * layout.
 *
 * Every top-level entry has to be accounted for in one of three ways: owned by a module declared in
 * `settings.gradle.kts`, listed as an always-full path, or named here as having no build impact.
 * Adding a new top-level directory without classifying it fails this test.
 *
 * Nothing here can under-select at runtime — an unclassified path belongs to no module and already
 * forces a full run. What the test protects against is the silent version of that: a directory that
 * forces full runs forever because nobody noticed it needed classifying.
 *
 * The lists are read out of the build files as text rather than from the configured Gradle model,
 * which keeps the test a plain unit test at the cost of depending on their formatting. The
 * assertions below fail loudly if either list stops parsing.
 */
class AffectedModulesConfigTest {

    private val repoRoot = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile && File(it, "app").isDirectory }

    /**
     * Top-level entries that change nothing the build produces: editor and agent configuration,
     * licences, documentation, secret-scanning and reporting helpers. A change to one of these
     * still forces a full run today, because it belongs to no module — listing it here records
     * that we know, and is the hook for skipping it once the detector supports ignored paths.
     */
    private val noBuildImpact = setOf(
        ".claude",
        ".cursor",
        ".github",
        ".gitignore",
        ".gitleaks",
        ".gitleaksignore",
        ".idea",
        ".testrail",
        "CREDITS.md",
        "LICENCE.md",
        "README.md",
    )

    private val declaredModuleRoots: Set<String> by lazy {
        val includes = Regex("""include\("(:[^"]+)"\)""")
        includes.findAll(File(repoRoot, "settings.gradle.kts").readText())
            .map { it.groupValues[1].removePrefix(":").substringBefore(':') }
            .toSet()
    }

    private val alwaysFullPaths: Set<String> by lazy {
        val block = Regex("""alwaysFullPaths\s*=\s*listOf\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
            .find(File(repoRoot, "build.gradle.kts").readText())
            ?.groupValues
            ?.get(1)
            .orEmpty()
        Regex("\"([^\"]+)\"").findAll(block).map { it.groupValues[1] }.toSet()
    }

    /**
     * Tracked top-level entries, read from git rather than from the filesystem. Scanning the working
     * directory would pick up generated output such as `.gradle` and fail for reasons that have
     * nothing to do with the configuration.
     */
    private val topLevelEntries: Set<String> by lazy {
        val process = ProcessBuilder("git", "ls-tree", "--name-only", "HEAD")
            .directory(repoRoot)
            .redirectErrorStream(true)
            .start()
        val entries = process.inputStream.bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        check(process.waitFor() == 0) { "git ls-tree failed in $repoRoot" }
        entries
    }

    @Test
    fun `test that the tracked top-level listing is not empty`() {
        assertThat(topLevelEntries).containsAtLeast("app", "settings.gradle.kts", "build.gradle.kts")
    }

    @Test
    fun `test that the module set parses out of settings`() {
        assertThat(declaredModuleRoots).containsAtLeast("app", "domain", "data", "feature")
    }

    @Test
    fun `test that the always-full list parses out of the root build file`() {
        assertThat(alwaysFullPaths).containsAtLeast("build-logic", "settings.gradle.kts", "gradle")
    }

    @Test
    fun `test that every top-level path is classified`() {
        val unclassified = topLevelEntries - declaredModuleRoots - alwaysFullPaths - noBuildImpact

        assertThat(unclassified).isEmpty()
    }

    @Test
    fun `test that every always-full path exists`() {
        val missing = alwaysFullPaths.filterNot { File(repoRoot, it).exists() }

        assertThat(missing).isEmpty()
    }

    @Test
    fun `test that no always-full path is also owned by a module`() {
        val overlapping = alwaysFullPaths intersect declaredModuleRoots

        assertThat(overlapping).isEmpty()
    }

    @Test
    fun `test that the build configuration files that shape every module are always-full`() {
        assertThat(alwaysFullPaths).containsAtLeast(
            "build-logic",
            "build.gradle.kts",
            "buildSrc",
            "gradle",
            "gradle.properties",
            "settings.gradle.kts",
        )
    }
}
