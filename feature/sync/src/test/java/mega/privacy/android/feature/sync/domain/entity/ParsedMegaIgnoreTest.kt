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
}
