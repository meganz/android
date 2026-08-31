package mega.privacy.android.feature.sync.data.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaIgnoreLineMapperTest {

    private val underTest = MegaIgnoreLineMapper()

    private fun rule(
        pattern: String,
        exclusionClass: SyncExclusionClass = SyncExclusionClass.Exclude,
        target: SyncExclusionTarget = SyncExclusionTarget.All,
        matchType: SyncExclusionMatchType = SyncExclusionMatchType.SubtreeName,
        strategy: SyncMatchStrategy = SyncMatchStrategy.GlobCaseInsensitive,
        enabled: Boolean = true,
    ) = SyncExclusionRule(
        pattern = pattern,
        exclusionClass = exclusionClass,
        target = target,
        matchType = matchType,
        strategy = strategy,
        enabled = enabled,
    )

    private fun parsedRule(line: String): SyncExclusionRule {
        val entry = underTest(line)
        assertThat(entry).isInstanceOf(MegaIgnoreEntry.RuleEntry::class.java)
        return (entry as MegaIgnoreEntry.RuleEntry).rule
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideClassLines")
    fun `test that invoke parses the class letter`(
        line: String,
        expected: SyncExclusionClass,
    ) {
        assertThat(parsedRule(line).exclusionClass).isEqualTo(expected)
    }

    private fun provideClassLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-:temp", SyncExclusionClass.Exclude),
        Arguments.of("+:temp", SyncExclusionClass.Include),
    )

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideTargetLines")
    fun `test that invoke parses the target letter`(
        line: String,
        expected: SyncExclusionTarget,
    ) {
        assertThat(parsedRule(line).target).isEqualTo(expected)
    }

    private fun provideTargetLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-:temp", SyncExclusionTarget.All),
        Arguments.of("-a:temp", SyncExclusionTarget.All),
        Arguments.of("-d:temp", SyncExclusionTarget.Directories),
        Arguments.of("-f:temp", SyncExclusionTarget.Files),
        Arguments.of("-s:temp", SyncExclusionTarget.Symlinks),
    )

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideMatchTypeLines")
    fun `test that invoke parses the type letter`(
        line: String,
        expected: SyncExclusionMatchType,
    ) {
        assertThat(parsedRule(line).matchType).isEqualTo(expected)
    }

    private fun provideMatchTypeLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-N:temp", SyncExclusionMatchType.LocalName),
        Arguments.of("-n:temp", SyncExclusionMatchType.SubtreeName),
        Arguments.of("-p:temp", SyncExclusionMatchType.Path),
        Arguments.of("-N:a/b", SyncExclusionMatchType.LocalName),
        Arguments.of("-n:a/b", SyncExclusionMatchType.SubtreeName),
        Arguments.of("-p:a/b", SyncExclusionMatchType.Path),
    )

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideInferredMatchTypeLines")
    fun `test that invoke infers the type from the pattern when the type letter is omitted`(
        line: String,
        expected: SyncExclusionMatchType,
    ) {
        assertThat(parsedRule(line).matchType).isEqualTo(expected)
    }

    private fun provideInferredMatchTypeLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-:temp", SyncExclusionMatchType.SubtreeName),
        Arguments.of("-:a/b", SyncExclusionMatchType.Path),
        Arguments.of("-:/leading", SyncExclusionMatchType.Path),
        Arguments.of("-:trailing/", SyncExclusionMatchType.Path),
        Arguments.of("-dG:a/b", SyncExclusionMatchType.Path),
    )

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideStrategyLines")
    fun `test that invoke parses the glob strategy letter`(
        line: String,
        expected: SyncMatchStrategy,
    ) {
        assertThat(parsedRule(line).strategy).isEqualTo(expected)
    }

    private fun provideStrategyLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-:temp", SyncMatchStrategy.GlobCaseInsensitive),
        Arguments.of("-g:temp", SyncMatchStrategy.GlobCaseInsensitive),
        Arguments.of("-G:temp", SyncMatchStrategy.GlobCaseSensitive),
    )

    @Test
    fun `test that invoke parses every grammar field of a fully qualified line`() {
        assertThat(parsedRule("+sNG:a/b")).isEqualTo(
            rule(
                pattern = "a/b",
                exclusionClass = SyncExclusionClass.Include,
                target = SyncExclusionTarget.Symlinks,
                matchType = SyncExclusionMatchType.LocalName,
                strategy = SyncMatchStrategy.GlobCaseSensitive,
            )
        )
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "-r:temp",
            "-R:temp",
            "-dr:temp",
            "+R:a/b",
            "#-r:temp",
        ]
    )
    fun `test that invoke returns null for a regex rule so the caller preserves it verbatim`(line: String) {
        assertThat(underTest(line)).isNull()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "",
            "   ",
            ":temp",
            "temp",
            "x:temp",
            "-temp",
            "-:",
            "-:   ",
            "-A:temp",
            "-D:temp",
            "-P:temp",
            "-gd:temp",
            "-Nd:temp",
            "-dz:temp",
            " -:temp",
            "+sync:.megaignore",
            "-sync:.megaignore",
            "+sync:.megaignore ",
            "+sync:.megaignoreX",
            "-sync:.megaignor",
            "+SYNC:.megaignore",
        ]
    )
    fun `test that invoke returns null for a line it cannot model`(line: String) {
        assertThat(underTest(line)).isNull()
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @MethodSource("providePatternLines")
    fun `test that invoke strips trailing whitespace from the pattern but keeps leading whitespace`(
        line: String,
        expected: String,
    ) {
        assertThat(parsedRule(line).pattern).isEqualTo(expected)
    }

    private fun providePatternLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-:temp", "temp"),
        Arguments.of("-:temp   ", "temp"),
        Arguments.of("-:temp\t", "temp"),
        Arguments.of("-:  temp", "  temp"),
        Arguments.of("-:a:b", "a:b"),
        Arguments.of("-:*.tmp", "*.tmp"),
        Arguments.of("-:temp\u000B", "temp"),
        Arguments.of("-:temp\u000C", "temp"),
        Arguments.of("-:temp \t ", "temp"),
    )

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @MethodSource("provideNonAsciiWhitespacePatternLines")
    fun `test that invoke keeps trailing whitespace the SDK treats as pattern content`(
        line: String,
        expected: String,
    ) {
        assertThat(parsedRule(line).pattern).isEqualTo(expected)
    }

    private fun provideNonAsciiWhitespacePatternLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-:temp\u00A0", "temp\u00A0"),
        Arguments.of("-:temp\u3000", "temp\u3000"),
        Arguments.of("-:temp\u001C", "temp\u001C"),
        Arguments.of("-:temp\u00A0\u00A0", "temp\u00A0\u00A0"),
        Arguments.of("-:temp\u00A0 ", "temp\u00A0"),
    )

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "-:temp\u00A0",
            "-:temp\u3000",
            "-N:temp\u00A0",
            "#-d:temp\u00A0",
            "-:a/b\u00A0",
        ]
    )
    fun `test that a pattern ending in non-ASCII whitespace survives a round trip unchanged`(
        line: String,
    ) {
        val entry = underTest(line)

        assertThat(entry).isNotNull()
        assertThat(underTest(entry!!)).isEqualTo(line)
    }

    @Test
    fun `test that invoke parses a commented rule as a disabled rule`() {
        assertThat(parsedRule("#-d:temp")).isEqualTo(
            rule(pattern = "temp", target = SyncExclusionTarget.Directories, enabled = false)
        )
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "#",
            "# -:temp",
            "#  a plain comment",
            "#exclude-smaller:100",
            "##-:temp",
            "#-:",
        ]
    )
    fun `test that invoke returns null for a comment that is not a disabled rule`(line: String) {
        assertThat(underTest(line)).isNull()
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("provideSizeLines")
    fun `test that invoke parses a size directive`(line: String, expected: MegaIgnoreEntry) {
        assertThat(underTest(line)).isEqualTo(expected)
    }

    private fun provideSizeLines(): Stream<Arguments> = Stream.of(
        Arguments.of("exclude-smaller:0", MegaIgnoreEntry.SizeSmallerEntry(0)),
        Arguments.of("exclude-smaller:100", MegaIgnoreEntry.SizeSmallerEntry(100)),
        Arguments.of("exclude-larger:100", MegaIgnoreEntry.SizeLargerEntry(100)),
        Arguments.of("exclude-larger: 100", MegaIgnoreEntry.SizeLargerEntry(100)),
        Arguments.of("exclude-larger:100  ", MegaIgnoreEntry.SizeLargerEntry(100)),
        Arguments.of("exclude-larger:1k", MegaIgnoreEntry.SizeLargerEntry(1024)),
        Arguments.of("exclude-larger:1K", MegaIgnoreEntry.SizeLargerEntry(1024)),
        Arguments.of("exclude-larger:1m", MegaIgnoreEntry.SizeLargerEntry(1048576)),
        Arguments.of("exclude-larger:1M", MegaIgnoreEntry.SizeLargerEntry(1048576)),
        Arguments.of("exclude-larger:1g", MegaIgnoreEntry.SizeLargerEntry(1073741824)),
        Arguments.of("exclude-larger:1t", MegaIgnoreEntry.SizeLargerEntry(1099511627776)),
        Arguments.of("exclude-smaller:2k", MegaIgnoreEntry.SizeSmallerEntry(2048)),
    )

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "exclude-larger:",
            "exclude-larger:   ",
            "exclude-larger:abc",
            "exclude-larger:-5",
            "exclude-larger:+5",
            "exclude-larger:1.5k",
            "exclude-larger:1kb",
            "exclude-larger:1x",
            "exclude-larger:k",
            "exclude-larger:1 k",
            "Exclude-Larger:5",
            "EXCLUDE-SMALLER:5",
            "exclude-larger",
            "exclude-other:5",
            "exclude-larger:١٠",
        ]
    )
    fun `test that invoke returns null for a malformed size directive`(line: String) {
        assertThat(underTest(line)).isNull()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "exclude-larger:9223372036854775808",
            "exclude-larger:99999999999999999999999",
            "exclude-larger:9223372036854775807k",
            "exclude-larger:8388608t",
        ]
    )
    fun `test that invoke returns null when a size value overflows`(line: String) {
        assertThat(underTest(line)).isNull()
    }

    @Test
    fun `test that invoke parses the largest representable size value`() {
        assertThat(underTest("exclude-larger:9223372036854775807"))
            .isEqualTo(MegaIgnoreEntry.SizeLargerEntry(Long.MAX_VALUE))
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @MethodSource("provideSerializedRules")
    fun `test that invoke serializes a rule in its minimal round-trip safe form`(
        rule: SyncExclusionRule,
        expected: String,
    ) {
        assertThat(underTest(MegaIgnoreEntry.RuleEntry(rule))).isEqualTo(expected)
    }

    private fun provideSerializedRules(): Stream<Arguments> = Stream.of(
        Arguments.of(rule("temp"), "-:temp"),
        Arguments.of(rule("temp", exclusionClass = SyncExclusionClass.Include), "+:temp"),
        Arguments.of(rule("temp", target = SyncExclusionTarget.All), "-:temp"),
        Arguments.of(rule("temp", target = SyncExclusionTarget.Directories), "-d:temp"),
        Arguments.of(rule("temp", target = SyncExclusionTarget.Files), "-f:temp"),
        Arguments.of(rule("temp", target = SyncExclusionTarget.Symlinks), "-s:temp"),
        Arguments.of(rule("temp", matchType = SyncExclusionMatchType.LocalName), "-N:temp"),
        Arguments.of(rule("a/b", matchType = SyncExclusionMatchType.LocalName), "-N:a/b"),
        Arguments.of(rule("temp", matchType = SyncExclusionMatchType.SubtreeName), "-:temp"),
        Arguments.of(rule("a/b", matchType = SyncExclusionMatchType.SubtreeName), "-n:a/b"),
        Arguments.of(rule("a/b", matchType = SyncExclusionMatchType.Path), "-:a/b"),
        Arguments.of(rule("temp", matchType = SyncExclusionMatchType.Path), "-p:temp"),
        Arguments.of(rule("temp", strategy = SyncMatchStrategy.GlobCaseInsensitive), "-:temp"),
        Arguments.of(rule("temp", strategy = SyncMatchStrategy.GlobCaseSensitive), "-G:temp"),
        Arguments.of(rule("temp", strategy = SyncMatchStrategy.RegexCaseInsensitive), "-r:temp"),
        Arguments.of(rule("temp", strategy = SyncMatchStrategy.RegexCaseSensitive), "-R:temp"),
        Arguments.of(rule("temp", enabled = false), "#-:temp"),
        Arguments.of(
            rule(
                pattern = "a/b",
                exclusionClass = SyncExclusionClass.Include,
                target = SyncExclusionTarget.Symlinks,
                matchType = SyncExclusionMatchType.LocalName,
                strategy = SyncMatchStrategy.GlobCaseSensitive,
                enabled = false,
            ),
            "#+sNG:a/b",
        ),
    )

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @MethodSource("provideSerializedNonRuleEntries")
    fun `test that invoke serializes a non-rule entry`(entry: MegaIgnoreEntry, expected: String) {
        assertThat(underTest(entry)).isEqualTo(expected)
    }

    private fun provideSerializedNonRuleEntries(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaIgnoreEntry.SizeSmallerEntry(0), "exclude-smaller:0"),
        Arguments.of(MegaIgnoreEntry.SizeSmallerEntry(2048), "exclude-smaller:2048"),
        Arguments.of(MegaIgnoreEntry.SizeLargerEntry(1024), "exclude-larger:1024"),
        Arguments.of(MegaIgnoreEntry.SizeLargerEntry(Long.MAX_VALUE), "exclude-larger:9223372036854775807"),
        Arguments.of(MegaIgnoreEntry.PreservedEntry(""), ""),
        Arguments.of(MegaIgnoreEntry.PreservedEntry("# a plain comment"), "# a plain comment"),
        Arguments.of(MegaIgnoreEntry.PreservedEntry("+sync:.megaignore"), "+sync:.megaignore"),
        Arguments.of(MegaIgnoreEntry.PreservedEntry("-R:^cache$"), "-R:^cache$"),
    )

    @Test
    fun `test that invoke emits raw bytes for a size value that was read with a suffix`() {
        val parsed = underTest("exclude-larger:2k")

        assertThat(parsed).isNotNull()
        assertThat(underTest(parsed!!)).isEqualTo("exclude-larger:2048")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRoundTripRules")
    fun `test that a serialized rule parses back to the same rule`(rule: SyncExclusionRule) {
        val line = underTest(MegaIgnoreEntry.RuleEntry(rule))

        assertThat(underTest(line)).isEqualTo(MegaIgnoreEntry.RuleEntry(rule))
    }

    private fun provideRoundTripRules(): Stream<Arguments> =
        listOf("temp", "a/b", "*.tmp", "a/b/c.txt", "/absolute").flatMap { pattern ->
            SyncExclusionClass.entries.flatMap { exclusionClass ->
                SyncExclusionTarget.entries.flatMap { target ->
                    SyncExclusionMatchType.entries.flatMap { matchType ->
                        listOf(
                            SyncMatchStrategy.GlobCaseInsensitive,
                            SyncMatchStrategy.GlobCaseSensitive,
                        ).flatMap { strategy ->
                            listOf(true, false).map { enabled ->
                                Arguments.of(
                                    rule(
                                        pattern = pattern,
                                        exclusionClass = exclusionClass,
                                        target = target,
                                        matchType = matchType,
                                        strategy = strategy,
                                        enabled = enabled,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }.stream()

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "-:temp",
            "+:temp",
            "-d:temp",
            "-f:temp",
            "-s:temp",
            "-N:temp",
            "-n:a/b",
            "-p:temp",
            "-:a/b",
            "-G:temp",
            "#-:temp",
            "#+sNG:a/b",
            "exclude-smaller:100",
            "exclude-larger:2048",
        ]
    )
    fun `test that a canonical line survives a parse and serialize round trip`(line: String) {
        val entry = underTest(line)

        assertThat(entry).isNotNull()
        assertThat(underTest(entry!!)).isEqualTo(line)
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @MethodSource("provideNonCanonicalLines")
    fun `test that a non-canonical line is normalized by a parse and serialize round trip`(
        line: String,
        expected: String,
    ) {
        val entry = underTest(line)

        assertThat(entry).isNotNull()
        assertThat(underTest(entry!!)).isEqualTo(expected)
    }

    private fun provideNonCanonicalLines(): Stream<Arguments> = Stream.of(
        Arguments.of("-a:temp", "-:temp"),
        Arguments.of("-g:temp", "-:temp"),
        Arguments.of("-ag:temp", "-:temp"),
        Arguments.of("-n:temp", "-:temp"),
        Arguments.of("-p:a/b", "-:a/b"),
        Arguments.of("-:temp   ", "-:temp"),
        Arguments.of("exclude-larger:1k", "exclude-larger:1024"),
        Arguments.of("exclude-larger: 100", "exclude-larger:100"),
    )
}
