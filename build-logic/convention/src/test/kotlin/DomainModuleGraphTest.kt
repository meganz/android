import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps the `:domain` module tree in the shape the domain split relies on.
 *
 * `:domain`, `:domain:core` and `:domain:feature` are bare containers. `:domain:core:*` holds what
 * every area shares (today `common` and the cross-area residue in `shared`); `:domain:feature:*`
 * holds one business area each. A feature module may depend on core modules and on nothing else in
 * the domain tree — in particular never on another feature module, because a shared dependency
 * between two areas belongs in core, or should be inverted, rather than making one area reach into
 * the other. Core modules may not depend on feature modules at all.
 *
 * The graph is read out of the build files as text, like [AffectedModulesConfigTest], so this stays
 * a plain unit test. Only `project(":...")` references are considered.
 */
class DomainModuleGraphTest {

    private val repoRoot = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile && File(it, "app").isDirectory }

    private val domainModules: Set<String> by lazy {
        Regex("""include\("(:domain[^"]*)"\)""")
            .findAll(File(repoRoot, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun buildFile(module: String): File {
        val dir = File(repoRoot, module.removePrefix(":").replace(':', '/'))
        return File(dir, "${dir.name}.gradle.kts")
    }

    private fun projectDependencies(module: String): Set<String> =
        Regex("""project\("(:[^"]+)"\)""")
            .findAll(buildFile(module).readText())
            .map { it.groupValues[1] }
            .filterNot { it == ":lint" }
            .toSet()

    private val featureModules get() = domainModules.filter { it.startsWith(":domain:feature:") }
    private val coreModules get() = domainModules.filter { it.startsWith(":domain:core:") }

    @Test
    fun `test that the domain tree is declared in settings`() {
        assertThat(domainModules).containsAtLeast(":domain:core:common", ":domain:core:shared")
        assertThat(featureModules).isNotEmpty()
    }

    @Test
    fun `test that every domain module is a core or a feature module`() {
        val misplaced = domainModules.filterNot {
            it.startsWith(":domain:core:") || it.startsWith(":domain:feature:")
        }

        assertThat(misplaced).isEmpty()
    }

    @Test
    fun `test that the containers carry no build file`() {
        val containers = listOf(":domain", ":domain:core", ":domain:feature")

        assertThat(containers.filter { buildFile(it).exists() }).isEmpty()
    }

    @Test
    fun `test that every domain module has a build file named after its directory`() {
        val missing = domainModules.filterNot { buildFile(it).isFile }

        assertThat(missing).isEmpty()
    }

    @Test
    fun `test that no feature module depends on another feature module`() {
        val violations = featureModules.flatMap { module ->
            projectDependencies(module)
                .filter { it.startsWith(":domain:feature:") }
                .map { "$module -> $it" }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that no core module depends on a feature module`() {
        val violations = coreModules.flatMap { module ->
            projectDependencies(module)
                .filter { it.startsWith(":domain:feature:") }
                .map { "$module -> $it" }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that domain modules depend on nothing outside the domain tree`() {
        val violations = domainModules.flatMap { module ->
            projectDependencies(module)
                .filterNot { it.startsWith(":domain:") }
                .map { "$module -> $it" }
        }

        assertThat(violations).isEmpty()
    }
}
