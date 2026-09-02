package mega.privacy.android.feature.sync.domain.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ParsedMegaIgnoreTest {

    private fun rule(pattern: String) = SyncExclusionRule(
        pattern = pattern,
        matchType = SyncExclusionMatchType.SubtreeName,
    )

    @Test
    fun `test that rules projects rule entries in file order`() {
        val first = rule("*.tmp")
        val second = rule("*.log")

        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(first),
                MegaIgnoreEntry.PreservedEntry("# a comment"),
                MegaIgnoreEntry.RuleEntry(second),
            )
        )

        assertThat(underTest.rules).containsExactly(first, second).inOrder()
    }

    @Test
    fun `test that rules is empty when the file has no rule entries`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(MegaIgnoreEntry.PreservedEntry("# only a comment"))
        )

        assertThat(underTest.rules).isEmpty()
    }

    @Test
    fun `test that rules keeps a repeated rule as two separate entries`() {
        val repeated = rule("*.tmp")

        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(repeated),
                MegaIgnoreEntry.RuleEntry(repeated),
            )
        )

        assertThat(underTest.rules).hasSize(2)
    }

    @Test
    fun `test that rules distinguishes two rules differing only by enabled`() {
        val enabled = rule("*.tmp")
        val disabled = enabled.copy(enabled = false)

        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(enabled),
                MegaIgnoreEntry.RuleEntry(disabled),
            )
        )

        assertThat(underTest.rules).containsExactly(enabled, disabled).inOrder()
        assertThat(enabled).isNotEqualTo(disabled)
    }

    @Test
    fun `test that size derives the smaller bound from the last occurrence when it is repeated`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.SizeSmallerEntry(1_000L),
                MegaIgnoreEntry.PreservedEntry("# reset below"),
                MegaIgnoreEntry.SizeSmallerEntry(2_000L),
            )
        )

        assertThat(underTest.size.excludeSmallerThanBytes).isEqualTo(2_000L)
    }

    @Test
    fun `test that size derives the larger bound from the last occurrence when it is repeated`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.SizeLargerEntry(5_000L),
                MegaIgnoreEntry.SizeLargerEntry(9_000L),
            )
        )

        assertThat(underTest.size.excludeLargerThanBytes).isEqualTo(9_000L)
    }

    @Test
    fun `test that size derives each bound independently when both are repeated and interleaved`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.SizeSmallerEntry(100L),
                MegaIgnoreEntry.SizeLargerEntry(8_000L),
                MegaIgnoreEntry.SizeSmallerEntry(200L),
            )
        )

        assertThat(underTest.size).isEqualTo(
            SyncSizeExclusion(excludeSmallerThanBytes = 200L, excludeLargerThanBytes = 8_000L)
        )
    }

    @Test
    fun `test that size reports both bounds when both are present`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.SizeLargerEntry(9_000L),
                MegaIgnoreEntry.SizeSmallerEntry(10L),
            )
        )

        assertThat(underTest.size).isEqualTo(
            SyncSizeExclusion(excludeSmallerThanBytes = 10L, excludeLargerThanBytes = 9_000L)
        )
    }

    @Test
    fun `test that size is unbounded when the file has no size entries`() {
        val underTest = ParsedMegaIgnore(entries = listOf(MegaIgnoreEntry.RuleEntry(rule("*.tmp"))))

        assertThat(underTest.size.isUnbounded).isTrue()
    }

    @Test
    fun `test that preservedLines projects preserved entries in file order`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.RuleEntry(rule("*.tmp")),
                MegaIgnoreEntry.PreservedEntry("-:.*(cache)"),
            )
        )

        assertThat(underTest.preservedLines)
            .containsExactly("# header", "-:.*(cache)")
            .inOrder()
    }

    @Test
    fun `test that every projection reads the same interleaved file without crossing over`() {
        val glob = rule("*.tmp")
        val trailing = rule("*.log")

        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.RuleEntry(glob),
                MegaIgnoreEntry.SizeLargerEntry(9_000L),
                MegaIgnoreEntry.PreservedEntry("-R:^cache$"),
                MegaIgnoreEntry.PreservedEntry(""),
                MegaIgnoreEntry.RuleEntry(trailing),
                MegaIgnoreEntry.PreservedEntry("-sync:.megaignore"),
            )
        )

        assertThat(underTest.rules).containsExactly(glob, trailing).inOrder()
        assertThat(underTest.preservedLines)
            .containsExactly("# header", "-R:^cache$", "", "-sync:.megaignore")
            .inOrder()
        assertThat(underTest.size).isEqualTo(SyncSizeExclusion(excludeLargerThanBytes = 9_000L))
        assertThat(underTest.entries).hasSize(7)
    }

    @Test
    fun `test that hadBom is false when it is not specified`() {
        assertThat(ParsedMegaIgnore(entries = emptyList()).hadBom).isFalse()
    }

    @Test
    fun `test that an empty model has no rules and no bounds`() {
        val underTest = ParsedMegaIgnore(entries = emptyList())

        assertThat(underTest.rules).isEmpty()
        assertThat(underTest.preservedLines).isEmpty()
        assertThat(underTest.size.isUnbounded).isTrue()
    }
    // region invalid entries

    @Test
    fun `test that invalidLines projects invalid entries in file order`() {
        val first = invalidEntry("-A:temp")
        val second = invalidEntry("exclude-larger:x")

        val underTest = ParsedMegaIgnore(
            entries = listOf(first, MegaIgnoreEntry.RuleEntry(rule("*.tmp")), second)
        )

        assertThat(underTest.invalidLines.map { it.entry })
            .containsExactly(first, second)
            .inOrder()
    }

    @Test
    fun `test that invalidLines numbers an invalid entry by its position counting from one`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.RuleEntry(rule("*.tmp")),
                invalidEntry("-A:temp"),
            )
        )

        assertThat(underTest.invalidLines.single().lineNumber).isEqualTo(3)
    }

    /**
     * The number cannot be held on the entry: [ParsedMegaIgnore.entries] is what an edit rewrites,
     * and a stored number would keep pointing at the line the bad one used to be on.
     */
    @Test
    fun `test that invalidLines renumbers when an earlier entry is removed`() {
        val invalid = invalidEntry("-A:temp")
        val underTest = ParsedMegaIgnore(
            entries = listOf(MegaIgnoreEntry.RuleEntry(rule("*.tmp")), invalid)
        )

        val edited = underTest.copy(entries = underTest.entries - setOf(underTest.entries.first()))

        assertThat(underTest.invalidLines.single().lineNumber).isEqualTo(2)
        assertThat(edited.invalidLines.single().lineNumber).isEqualTo(1)
    }

    @Test
    fun `test that isRejectedBySdk is true when a single line is invalid`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(rule("*.tmp")),
                invalidEntry("-A:temp"),
            )
        )

        assertThat(underTest.isRejectedBySdk).isTrue()
    }

    /**
     * A line the app cannot model is not a line the SDK refuses, and only the latter makes the
     * file inert.
     */
    @Test
    fun `test that isRejectedBySdk is false for a file whose unmodelled lines are all preserved`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("-R:^cache$"),
                MegaIgnoreEntry.PreservedEntry("# a comment"),
                MegaIgnoreEntry.RuleEntry(rule("*.tmp")),
            )
        )

        assertThat(underTest.isRejectedBySdk).isFalse()
        assertThat(underTest.invalidLines).isEmpty()
    }

    @Test
    fun `test that an empty model is not rejected by the sdk`() {
        assertThat(ParsedMegaIgnore(entries = emptyList()).isRejectedBySdk).isFalse()
    }

    @Test
    fun `test that preservedLines does not report an invalid line`() {
        val underTest = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# a comment"),
                invalidEntry("-A:temp"),
            )
        )

        assertThat(underTest.preservedLines).containsExactly("# a comment")
    }

    /**
     * The rules of a refused file still parse, and still have to be reported, so that the user can
     * be shown the file they have alongside the news that none of it is in effect.
     */
    @Test
    fun `test that rules still reports the rules of a file the sdk refuses`() {
        val kept = rule("*.tmp")

        val underTest = ParsedMegaIgnore(
            entries = listOf(MegaIgnoreEntry.RuleEntry(kept), invalidEntry("-A:temp"))
        )

        assertThat(underTest.rules).containsExactly(kept)
        assertThat(underTest.isRejectedBySdk).isTrue()
    }

    private fun invalidEntry(line: String) = MegaIgnoreEntry.InvalidEntry(
        line = line,
        reason = InvalidLineReason.UnexpectedFlagCharacter,
    )

    // endregion
}
