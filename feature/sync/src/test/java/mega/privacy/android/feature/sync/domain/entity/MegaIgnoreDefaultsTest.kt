package mega.privacy.android.feature.sync.domain.entity

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreLineMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

/**
 * A parity test against the SDK's predefined exclusions, which is why it reaches across to
 * [MegaIgnoreLineMapper]: what has to hold is that a default survives the exact line the SDK writes
 * for it, not merely that the constant holds the right strings.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaIgnoreDefaultsTest {

    private val lineMapper = MegaIgnoreLineMapper()

    @Test
    fun `test that the pinned sdk fixture holds eight patterns`() {
        assertThat(sdkFixture).hasSize(8)
    }

    @Test
    fun `test that the default patterns match the pinned sdk fixture in order`() {
        assertThat(MegaIgnoreDefaults.RULES.map { it.pattern })
            .containsExactlyElementsIn(sdkFixture)
            .inOrder()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("provideSdkPatterns")
    fun `test that every default rule carries the grammar the sdk implies`(pattern: String) {
        assertThat(defaultRuleFor(pattern)).isEqualTo(
            SyncExclusionRule(
                pattern = pattern,
                exclusionClass = SyncExclusionClass.Exclude,
                target = SyncExclusionTarget.All,
                matchType = SyncExclusionMatchType.SubtreeName,
                strategy = SyncMatchStrategy.GlobCaseInsensitive,
                enabled = true,
            )
        )
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("provideSdkPatterns")
    fun `test that every default rule serializes to the line shape the sdk writes`(pattern: String) {
        val line = lineMapper(MegaIgnoreEntry.RuleEntry(defaultRuleFor(pattern)))

        assertThat(line).isEqualTo("-:$pattern")
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("provideSdkPatterns")
    fun `test that a rule parsed from the line the sdk writes is a default`(pattern: String) {
        val parsed = lineMapper("-:$pattern") as? MegaIgnoreEntry.RuleEntry

        assertThat(parsed).isEqualTo(MegaIgnoreEntry.RuleEntry(defaultRuleFor(pattern)))
        assertThat(MegaIgnoreDefaults.isDefault(checkNotNull(parsed).rule)).isTrue()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("provideSdkPatterns")
    fun `test that isDefault is true when a default rule is disabled`(pattern: String) {
        val disabled = defaultRuleFor(pattern).copy(enabled = false)

        assertThat(MegaIgnoreDefaults.isDefault(disabled)).isTrue()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["Thumbs.dbx", "node_modules", "*.tm", ".*.*", "*.crdownloadx"])
    fun `test that isDefault is false for a pattern that is not predefined`(pattern: String) {
        assertThat(MegaIgnoreDefaults.isDefault(ruleFor(pattern))).isFalse()
    }

    @Test
    fun `test that isDefault is false for a pattern that differs only in case`() {
        assertThat(MegaIgnoreDefaults.isDefault(ruleFor("thumbs.db"))).isFalse()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRulesWithOneGrammarFieldChanged")
    fun `test that isDefault is false when a grammar field differs`(
        label: String,
        rule: SyncExclusionRule,
    ) {
        assertThat(MegaIgnoreDefaults.isDefault(rule)).isFalse()
    }

    private fun provideSdkPatterns(): Stream<String> = sdkFixture.stream()

    private fun provideRulesWithOneGrammarFieldChanged(): Stream<Arguments> {
        val default = defaultRuleFor("Thumbs.db")
        return Stream.of(
            Arguments.of("include instead of exclude", default.copy(exclusionClass = SyncExclusionClass.Include)),
            Arguments.of("directories only", default.copy(target = SyncExclusionTarget.Directories)),
            Arguments.of("files only", default.copy(target = SyncExclusionTarget.Files)),
            Arguments.of("symlinks only", default.copy(target = SyncExclusionTarget.Symlinks)),
            Arguments.of("this folder only", default.copy(matchType = SyncExclusionMatchType.LocalName)),
            Arguments.of("path match", default.copy(matchType = SyncExclusionMatchType.Path)),
            Arguments.of("case sensitive glob", default.copy(strategy = SyncMatchStrategy.GlobCaseSensitive)),
        )
    }

    private fun defaultRuleFor(pattern: String): SyncExclusionRule =
        MegaIgnoreDefaults.RULES.single { it.pattern == pattern }

    private fun ruleFor(pattern: String): SyncExclusionRule = SyncExclusionRule(
        pattern = pattern,
        matchType = SyncExclusionMatchType.SubtreeName,
    )

    /**
     * The header has to be one block at the top: a pattern is taken verbatim, so a future default
     * beginning with [COMMENT_PREFIX] must not be mistaken for a comment and silently dropped,
     * which would leave the drift test green while the app seeded one exclusion too few.
     */
    private fun readFixturePatterns(): List<String> {
        val lines = checkNotNull(MegaIgnoreDefaultsTest::class.java.getResourceAsStream(FIXTURE)) {
            "Missing test fixture $FIXTURE"
        }.bufferedReader().use { reader -> reader.readLines() }

        val patterns = lines.dropWhile { it.isHeaderLine() }
        check(patterns.none { it.isHeaderLine() }) {
            "Comment or blank line found below the first pattern in $FIXTURE"
        }
        return patterns
    }

    private fun String.isHeaderLine(): Boolean = startsWith(COMMENT_PREFIX) || isBlank()

    private val sdkFixture: List<String> = readFixturePatterns()

    private companion object {
        const val FIXTURE = "/megaignore/sdk-predefined-name-exclusions.txt"
        const val COMMENT_PREFIX = "#"
    }
}
