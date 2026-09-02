package mega.privacy.android.feature.sync.data.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaIgnoreFileMapperTest {

    private val underTest = MegaIgnoreFileMapper(MegaIgnoreLineMapper())

    @Test
    fun `test that invoke parses an empty file to an empty model`() {
        val result = underTest("")

        assertThat(result.entries).isEmpty()
        assertThat(result.hadBom).isFalse()
    }

    @Test
    fun `test that invoke parses a bom only file to an empty model`() {
        val result = underTest(BOM)

        assertThat(result.entries).isEmpty()
        assertThat(result.hadBom).isTrue()
    }

    @Test
    fun `test that invoke preserves the order of an interleaved file`() {
        val result = underTest(interleavedFile)

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.RuleEntry(
                SyncExclusionRule(
                    pattern = "*.tmp",
                    matchType = SyncExclusionMatchType.SubtreeName,
                )
            ),
            MegaIgnoreEntry.SizeLargerEntry(104857600L),
            MegaIgnoreEntry.PreservedEntry("+r:^cache/.*\$"),
            MegaIgnoreEntry.RuleEntry(
                SyncExclusionRule(
                    pattern = "node_modules",
                    target = SyncExclusionTarget.Directories,
                    matchType = SyncExclusionMatchType.SubtreeName,
                )
            ),
            MegaIgnoreEntry.PreservedEntry("# keep secrets out of the sync"),
        ).inOrder()
    }

    @Test
    fun `test that invoke maps a lone line feed to a single blank preserved line`() {
        val result = underTest(LF)

        assertThat(result.entries).containsExactly(MegaIgnoreEntry.PreservedEntry(""))
    }

    @Test
    fun `test that invoke keeps a blank line between two rules`() {
        val result = underTest("-:a${LF}${LF}-:b")

        assertThat(result.entries).containsExactly(
            ruleEntry("a"),
            MegaIgnoreEntry.PreservedEntry(""),
            ruleEntry("b"),
        ).inOrder()
    }

    @Test
    fun `test that invoke drops only the terminator when the file ends with a line feed`() {
        val result = underTest("-:a${LF}")

        assertThat(result.entries).containsExactly(ruleEntry("a"))
    }

    @Test
    fun `test that invoke keeps a blank line that precedes the trailing line feed`() {
        val result = underTest("-:a${LF}${LF}${LF}")

        assertThat(result.entries).containsExactly(
            ruleEntry("a"),
            MegaIgnoreEntry.PreservedEntry(""),
            MegaIgnoreEntry.PreservedEntry(""),
        ).inOrder()
    }

    @Test
    fun `test that invoke records a bom and keeps the first line intact`() {
        val result = underTest("${BOM}-:a")

        assertThat(result.hadBom).isTrue()
        assertThat(result.entries).containsExactly(ruleEntry("a"))
    }

    @Test
    fun `test that invoke does not record a bom when the file has none`() {
        val result = underTest("-:a")

        assertThat(result.hadBom).isFalse()
    }

    @Test
    fun `test that invoke treats a byte order mark that is not leading as pattern content`() {
        val result = underTest("-:a${LF}-:${BOM}b")

        assertThat(result.hadBom).isFalse()
        assertThat(result.entries).containsExactly(
            ruleEntry("a"),
            ruleEntry("${BOM}b"),
        ).inOrder()
    }

    @Test
    fun `test that invoke tolerates carriage return line feed endings`() {
        val result = underTest("-:a${CR}${LF}-:b")

        assertThat(result.entries).containsExactly(ruleEntry("a"), ruleEntry("b")).inOrder()
    }

    @Test
    fun `test that invoke strips the carriage return from a preserved line`() {
        val result = underTest("# note${CR}${LF}-:a")

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.PreservedEntry("# note"),
            ruleEntry("a"),
        ).inOrder()
    }

    @Test
    fun `test that invoke treats a lone carriage return as a line break`() {
        val result = underTest("-:a${CR}-:b")

        assertThat(result.entries).containsExactly(ruleEntry("a"), ruleEntry("b")).inOrder()
    }

    @Test
    fun `test that invoke treats a run of carriage returns as line breaks`() {
        val result = underTest("# note${CR}${CR}${LF}")

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.PreservedEntry("# note"),
            MegaIgnoreEntry.PreservedEntry(""),
        ).inOrder()
    }

    @Test
    fun `test that invoke tolerates a carriage return at the end of the file`() {
        val result = underTest("-:a${CR}")

        assertThat(result.entries).containsExactly(ruleEntry("a"))
    }

    @Test
    fun `test that invoke preserves a regex rule verbatim`() {
        val result = underTest("+r:^cache/.*\$")

        assertThat(result.entries)
            .containsExactly(MegaIgnoreEntry.PreservedEntry("+r:^cache/.*\$"))
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["+sync:.megaignore", "-sync:.megaignore"])
    fun `test that invoke preserves the megaignore sync directive verbatim`(line: String) {
        val result = underTest(line)

        assertThat(result.entries).containsExactly(MegaIgnoreEntry.PreservedEntry(line))
        assertThat(underTest(result)).isEqualTo(line)
    }

    @Test
    fun `test that invoke maps both size lines to size entries`() {
        val result = underTest("exclude-smaller:1024${LF}exclude-larger:2048")

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.SizeSmallerEntry(1024L),
            MegaIgnoreEntry.SizeLargerEntry(2048L),
        ).inOrder()
    }

    @Test
    fun `test that invoke keeps every size line so that the last bound wins`() {
        val result = underTest("exclude-larger:1024${LF}-:a${LF}exclude-larger:4096")

        assertThat(result.entries).hasSize(3)
        assertThat(result.size.excludeLargerThanBytes).isEqualTo(4096L)
    }

    @Test
    fun `test that invoke serializes an empty model to an empty string`() {
        val result = underTest(ParsedMegaIgnore(entries = emptyList()))

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that invoke serializes an empty model that had a bom to the bom alone`() {
        val result = underTest(ParsedMegaIgnore(entries = emptyList(), hadBom = true))

        assertThat(result).isEqualTo(BOM)
    }

    @Test
    fun `test that invoke joins entries with line feeds and adds no trailing newline`() {
        val result = underTest(
            ParsedMegaIgnore(
                entries = listOf(
                    ruleEntry("a"),
                    MegaIgnoreEntry.SizeLargerEntry(2048L),
                    MegaIgnoreEntry.PreservedEntry("# note"),
                )
            )
        )

        assertThat(result).isEqualTo("-:a${LF}exclude-larger:2048${LF}# note")
    }

    @ParameterizedTest(name = "hadBom = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke prefixes the bom only when the model had one`(hadBom: Boolean) {
        val result = underTest(
            ParsedMegaIgnore(entries = listOf(ruleEntry("a")), hadBom = hadBom)
        )

        assertThat(result.startsWith(BOM)).isEqualTo(hadBom)
        assertThat(result.endsWith("-:a")).isTrue()
    }

    @Test
    fun `test that invoke never emits a megaignore sync directive for a model built from scratch`() {
        val result = underTest(
            ParsedMegaIgnore(
                entries = listOf(ruleEntry("a"), MegaIgnoreEntry.SizeLargerEntry(2048L))
            )
        )

        assertThat(result).doesNotContain("sync:.megaignore")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCanonicalFiles")
    fun `test that invoke round trips a canonical file byte identically`(content: String) {
        assertThat(roundTrip(content)).isEqualTo(content)
    }

    @Test
    fun `test that invoke round trips the interleaved fixture byte identically`() {
        assertThat(roundTrip(interleavedFile)).isEqualTo(interleavedFile)
    }

    @Test
    fun `test that invoke round trips the interleaved fixture byte identically when it begins with a bom`() {
        val content = "${BOM}${interleavedFile}"

        assertThat(roundTrip(content)).isEqualTo(content)
    }

    @Test
    fun `test that invoke round trips a file in the shape the sdk writes on windows`() {
        val content =
            "${BOM}+sync:.megaignore${CR}${LF}exclude-larger:2048${CR}${LF}-:*.tmp${CR}${LF}"

        assertThat(roundTrip(content))
            .isEqualTo("${BOM}+sync:.megaignore${LF}exclude-larger:2048${LF}-:*.tmp")
    }

    @Test
    fun `test that invoke drops one trailing blank line per round trip`() {
        assertThat(roundTrip("-:a${LF}${LF}")).isEqualTo("-:a${LF}")
        assertThat(roundTrip("-:a${LF}")).isEqualTo("-:a")
    }

    @ParameterizedTest(name = "hadBom = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke round trips the bom flag through text`(hadBom: Boolean) {
        val model = ParsedMegaIgnore(entries = listOf(ruleEntry("a")), hadBom = hadBom)

        assertThat(underTest(underTest(model))).isEqualTo(model)
    }

    @Test
    fun `test that invoke normalizes a suffixed size line to bytes on round trip`() {
        assertThat(roundTrip("exclude-larger:100M")).isEqualTo("exclude-larger:104857600")
    }

    @Test
    fun `test that invoke normalizes carriage return line feed endings on round trip`() {
        assertThat(roundTrip("-:a${CR}${LF}-:b")).isEqualTo("-:a${LF}-:b")
    }

    @Test
    fun `test that invoke drops a trailing line feed on round trip`() {
        assertThat(roundTrip("-:a${LF}")).isEqualTo("-:a")
    }

    private fun provideCanonicalFiles(): Stream<String> = Stream.of(
        "",
        "-:*.tmp",
        "-:a${LF}-:b${LF}-:c",
        "exclude-smaller:1${LF}exclude-smaller:2",
        "-:a${LF}${LF}-:b",
        "#-:disabled${LF}-:a",
        "${BOM}-:a${LF}# note",
    )

    private fun roundTrip(content: String): String = underTest(underTest(content))

    private fun ruleEntry(pattern: String) = MegaIgnoreEntry.RuleEntry(
        SyncExclusionRule(pattern = pattern, matchType = SyncExclusionMatchType.SubtreeName)
    )

    private val interleavedFile = listOf(
        "-:*.tmp",
        "exclude-larger:104857600",
        "+r:^cache/.*\$",
        "-d:node_modules",
        "# keep secrets out of the sync",
    ).joinToString(LF)

    private companion object {
        const val LF = "\n"
        const val CR = "\r"
        const val BOM = "\uFEFF"
    }
    // region malformed lines

    @Test
    fun `test that invoke maps a line the sdk refuses to an invalid entry`() {
        val result = underTest("-A:temp")

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.InvalidEntry(
                line = "-A:temp",
                reason = InvalidLineReason.UnexpectedFlagCharacter,
            )
        )
    }

    @Test
    fun `test that invoke numbers an invalid line from one counting blank and comment lines`() {
        val result = underTest("# header${LF}${LF}-:a${LF} -:indented")

        assertThat(result.invalidLines.single().lineNumber).isEqualTo(4)
    }

    @Test
    fun `test that invoke numbers an invalid line from one when the file begins with a bom`() {
        val result = underTest("${BOM}-:a${LF}-A:temp")

        assertThat(result.invalidLines.single().lineNumber).isEqualTo(2)
    }

    @Test
    fun `test that invoke keeps an invalid line in position between the rules around it`() {
        val result = underTest("-:a${LF}-A:temp${LF}-:b")

        assertThat(result.entries).containsExactly(
            ruleEntry("a"),
            MegaIgnoreEntry.InvalidEntry("-A:temp", InvalidLineReason.UnexpectedFlagCharacter),
            ruleEntry("b"),
        ).inOrder()
    }

    @Test
    fun `test that invoke round trips a file holding an invalid line byte identically`() {
        val content = "-:a${LF} # indented comment${LF}-A:temp${LF}exclude-larger:100 ${LF}-:b"

        assertThat(roundTrip(content)).isEqualTo(content)
    }

    @Test
    fun `test that invoke reports the file as refused by the sdk when a line is invalid`() {
        assertThat(underTest("-:a${LF}-A:temp").isRejectedBySdk).isTrue()
    }

    /**
     * The whole point of the split: these are lines the app cannot model but the SDK parses, so a
     * file made only of them is being enforced and must not be reported as refused.
     */
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            "+r:^cache/.*\$",
            "-R:^cache\$",
            "# a plain comment",
            "   ",
            "+sync:.megaignore",
            "-sync:.megaignore",
            "-:",
            "exclude-larger:18446744073709551615",
        ]
    )
    fun `test that invoke does not report a line it merely cannot model as invalid`(line: String) {
        val result = underTest("-:a${LF}$line")

        assertThat(result.isRejectedBySdk).isFalse()
        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.PreservedEntry(line))
    }

    @Test
    fun `test that invoke does not report a valid file as refused by the sdk`() {
        val result = underTest(interleavedFile)

        assertThat(result.isRejectedBySdk).isFalse()
        assertThat(result.invalidLines).isEmpty()
    }

    // endregion

    // region size bounds

    /**
     * The SDK checks the bounds as it applies each line, so the file is refused at the line that
     * makes them equal even though the bound moves apart again straight after.
     */
    @Test
    fun `test that invoke flags the size line that makes the bounds equal`() {
        val result =
            underTest("exclude-smaller:100${LF}exclude-larger:100${LF}exclude-larger:200")

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.SizeSmallerEntry(100L),
            MegaIgnoreEntry.InvalidEntry(
                line = "exclude-larger:100",
                reason = InvalidLineReason.EqualSizeBounds,
            ),
            MegaIgnoreEntry.SizeLargerEntry(200L),
        ).inOrder()
    }

    /**
     * Both bounds start wide open at zero and the unsigned 64-bit maximum, so a line that sets
     * either end to the other end's starting value is already equal bounds.
     */
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["exclude-larger:0", "exclude-smaller:18446744073709551615"])
    fun `test that invoke flags a lone size line that collides with an untouched bound`(
        line: String,
    ) {
        val result = underTest(line)

        assertThat(result.invalidLines.single().entry.reason)
            .isEqualTo(InvalidLineReason.EqualSizeBounds)
    }

    @Test
    fun `test that invoke accepts size bounds that never meet`() {
        val result = underTest("exclude-smaller:100${LF}exclude-larger:200")

        assertThat(result.isRejectedBySdk).isFalse()
    }

    @Test
    fun `test that invoke accepts a lower bound above the upper bound`() {
        val result = underTest("exclude-smaller:200${LF}exclude-larger:100")

        assertThat(result.isRejectedBySdk).isFalse()
    }

    @Test
    fun `test that invoke compares bounds in bytes rather than as written`() {
        val result = underTest("exclude-smaller:1024${LF}exclude-larger:1k")

        assertThat(result.invalidLines.single().lineNumber).isEqualTo(2)
    }

    /**
     * A bound wider than a [Long] is preserved rather than modelled, and still has to take part
     * in the comparison: the SDK applied it, so it is the bound in force.
     */
    @Test
    fun `test that invoke folds a size bound too wide for the model into the bounds`() {
        val result = underTest(
            "exclude-smaller:9223372036854775808${LF}exclude-larger:9223372036854775808"
        )

        assertThat(result.entries.first())
            .isEqualTo(MegaIgnoreEntry.PreservedEntry("exclude-smaller:9223372036854775808"))
        assertThat(result.invalidLines.single().lineNumber).isEqualTo(2)
    }

    /**
     * The SDK abandons the load at the line it refuses, so it never reads the size lines after it
     * and the bound that line left behind is not one it ever applied. Carrying it forward would
     * blame the next size line for a collision with a bound that was never in force: on its own,
     * after a legal upper bound, `exclude-smaller:0` is accepted.
     */
    @Test
    fun `test that invoke stops folding size bounds once a size line has been rejected`() {
        val result = underTest("exclude-larger:0${LF}exclude-smaller:0")

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.SizeSmallerEntry(0L))
    }

    @Test
    fun `test that invoke stops folding size bounds once any line has been rejected`() {
        val result = underTest("-A:temp${LF}exclude-larger:0")

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.SizeLargerEntry(0L))
    }

    /**
     * Two identical bad lines are the same entry wherever they sit, so nothing about an entry may
     * depend on its position: that is the parsed list's own business and only it can keep it true
     * once an earlier line is edited away.
     */
    @Test
    fun `test that two identical invalid lines map to equal entries`() {
        val result = underTest("-A:temp${LF}-:a${LF}-A:temp")

        assertThat(result.entries.first()).isEqualTo(result.entries.last())
    }

    @Test
    fun `test that invoke numbers two identical invalid lines by their own positions`() {
        val result = underTest("-A:temp${LF}-:a${LF}-A:temp")

        assertThat(result.invalidLines.map { it.lineNumber }).containsExactly(1, 3).inOrder()
    }

    @Test
    fun `test that invoke does not flag repeated equal bounds of the same kind`() {
        val result = underTest("exclude-larger:100${LF}exclude-larger:100")

        assertThat(result.isRejectedBySdk).isFalse()
    }

    // endregion

}
