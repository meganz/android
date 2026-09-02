package mega.privacy.android.feature.sync.domain.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ParsedMegaIgnoreRuleIndexTest {

    @ParameterizedTest(name = "rule {0} is entry {1}")
    @CsvSource("0, 1", "1, 3", "2, 6")
    fun `test that the rule index resolves past every unmodelled line above it`(
        ruleIndex: Int,
        expectedEntriesIndex: Int,
    ) {
        assertThat(interleaved.entriesIndexOfRule(ruleIndex)).isEqualTo(expectedEntriesIndex)
    }

    @ParameterizedTest(name = "rule index {0}")
    @ValueSource(ints = [-1, 3, 100])
    fun `test that the rule index resolves to null when the file holds no rule there`(
        ruleIndex: Int,
    ) {
        assertThat(interleaved.entriesIndexOfRule(ruleIndex)).isNull()
    }

    @Test
    fun `test that the rule index resolves to null when the file holds no rules at all`() {
        val ruleless = ParsedMegaIgnore(entries = listOf(note, smaller))

        assertThat(ruleless.entriesIndexOfRule(0)).isNull()
    }

    @Test
    fun `test that the expected rule resolves when it is still the rule at the index`() {
        assertThat(interleaved.entriesIndexOfRule(ruleIndex = 1, expected = second)).isEqualTo(3)
    }

    @Test
    fun `test that the expected rule resolves to null when a different rule sits at the index`() {
        assertThat(interleaved.entriesIndexOfRule(ruleIndex = 1, expected = third)).isNull()
    }

    @Test
    fun `test that the expected rule resolves when the rule there differs only in enabled`() {
        assertThat(
            interleaved.entriesIndexOfRule(
                ruleIndex = 1,
                expected = second.copy(enabled = false),
            )
        ).isEqualTo(3)
    }

    @Test
    fun `test that the expected rule resolves to null when the rule there differs in another field`() {
        assertThat(
            interleaved.entriesIndexOfRule(
                ruleIndex = 1,
                expected = second.copy(matchType = SyncExclusionMatchType.LocalName),
            )
        ).isNull()
    }

    @Test
    fun `test that the expected rule resolves to null when the index is out of range`() {
        assertThat(interleaved.entriesIndexOfRule(ruleIndex = 3, expected = first)).isNull()
    }

    private val note = MegaIgnoreEntry.PreservedEntry("# a note the user typed")
    private val blank = MegaIgnoreEntry.PreservedEntry("")
    private val smaller = MegaIgnoreEntry.SizeSmallerEntry(1024L)
    private val larger = MegaIgnoreEntry.SizeLargerEntry(2048L)
    private val invalid = MegaIgnoreEntry.InvalidEntry(
        line = "-x:oops",
        reason = InvalidLineReason.UnexpectedFlagCharacter,
    )

    private val first = rule(pattern = "First*")
    private val second = rule(pattern = "Second*")
    private val third = rule(pattern = "Third*")

    private val interleaved = ParsedMegaIgnore(
        entries = listOf(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(second),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
        )
    )

    private fun rule(pattern: String) = SyncExclusionRule(
        pattern = pattern,
        matchType = SyncExclusionMatchType.SubtreeName,
    )
}
