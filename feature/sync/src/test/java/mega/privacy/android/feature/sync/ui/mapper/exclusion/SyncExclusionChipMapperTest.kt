package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionChipMapperTest {

    private val underTest = SyncExclusionChipMapper(
        patternMapper = SyncExclusionPatternMapper(),
    )

    private val rowMapper = SyncExclusionRowMapper(
        patternMapper = SyncExclusionPatternMapper(),
        supportingTextMapper = SyncExclusionSupportingTextMapper(),
    )

    @Test
    fun `test that the default set yields the three chips the design shows`() {
        val result = underTest(parsed(MegaIgnoreDefaults.RULES))

        assertThat(result).hasSize(3)
        assertThat(result.map { it.extension })
            .containsExactly("crdownload", "sb-????????-??????", "tmp")
            .inOrder()
    }

    @Test
    fun `test that a rule shown as a row is left out of the chips`() {
        val result = underTest(parsed(listOf(rule("*.tmp"), rule("draft*"))))

        assertThat(result.map { it.rule.pattern }).containsExactly("*.tmp")
    }

    @Test
    fun `test that a rule left to the rows still advances the rule index`() {
        val parsed = parsed(listOf(rule("draft*"), rule("*.tmp")))

        assertThat(underTest(parsed).single().ruleIndex).isEqualTo(1)
    }

    @Test
    fun `test that chips and rows claim exactly the indices the other does not`() {
        val parsed = parsed(partitionRules())

        val chipIndices = underTest(parsed).map { it.ruleIndex }
        val rowIndices = rowMapper(parsed).map { it.ruleIndex }

        assertThat(chipIndices).containsExactly(0, 2).inOrder()
        assertThat(rowIndices).containsExactly(1, 3, 4, 5).inOrder()
    }

    @Test
    fun `test that chips and rows together cover every rule index exactly once`() {
        val rules = partitionRules()
        val parsed = parsed(rules)

        val allIndices = underTest(parsed).map { it.ruleIndex } +
                rowMapper(parsed).map { it.ruleIndex }

        assertThat(allIndices.sorted()).isEqualTo(rules.indices.toList())
    }

    @Test
    fun `test that the rule index addresses the rule the file holds at that position`() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# a comment"),
                MegaIgnoreEntry.RuleEntry(rule("draft*")),
                MegaIgnoreEntry.RuleEntry(rule("*.tmp")),
                MegaIgnoreEntry.PreservedEntry("+sync:.megaignore"),
                MegaIgnoreEntry.RuleEntry(rule("*.log")),
            ),
        )

        underTest(parsed).forEach { chip ->
            assertThat(chip.rule).isEqualTo(parsed.rules[chip.ruleIndex])
        }
    }

    @Test
    fun `test that a disabled extension rule still yields a chip`() {
        val result = underTest(parsed(listOf(rule("*.tmp", enabled = false))))

        assertThat(result.single().rule.enabled).isFalse()
    }

    @Test
    fun `test that two extension rules differing only in enabled yield distinct indices`() {
        val parsed = parsed(listOf(rule("*.tmp"), rule("*.tmp", enabled = false)))

        assertThat(underTest(parsed).map { it.ruleIndex }).containsExactly(0, 1).inOrder()
    }

    @Test
    fun `test that an include rule is not shown as a chip`() {
        val parsed = parsed(listOf(rule("*.tmp", exclusionClass = SyncExclusionClass.Include)))

        assertThat(underTest(parsed)).isEmpty()
    }

    @Test
    fun `test that an extension rule narrowed to directories is not shown as a chip`() {
        val parsed = parsed(listOf(rule("*.tmp", target = SyncExclusionTarget.Directories)))

        assertThat(underTest(parsed)).isEmpty()
    }

    @Test
    fun `test that a preserved line does not advance the rule index`() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# a comment"),
                MegaIgnoreEntry.RuleEntry(rule("*.tmp")),
            ),
        )

        assertThat(underTest(parsed).single().ruleIndex).isEqualTo(0)
    }

    @Test
    fun `test that an empty file yields no chips`() {
        assertThat(underTest(ParsedMegaIgnore(entries = emptyList()))).isEmpty()
    }

    /**
     * Rules that split across both mappers, including two that the pattern mapper reads
     * differently depending on whether it is handed the rule or only its pattern: narrowed to
     * directories, and included rather than excluded, both still look like extensions by pattern
     * alone. Without those, a mapper that consulted the pattern instead of the rule would drop
     * them from both halves and the partition would still appear whole.
     */
    private fun partitionRules() = listOf(
        rule("*.tmp"),
        rule("draft*"),
        rule("*.log"),
        rule("Thumbs.db"),
        rule("*.tmp", target = SyncExclusionTarget.Directories),
        rule("*.jpg", exclusionClass = SyncExclusionClass.Include),
    )

    private fun parsed(rules: List<SyncExclusionRule>) = ParsedMegaIgnore(
        entries = rules.map { MegaIgnoreEntry.RuleEntry(it) },
    )

    private fun rule(
        pattern: String,
        exclusionClass: SyncExclusionClass = SyncExclusionClass.Exclude,
        target: SyncExclusionTarget = SyncExclusionTarget.All,
        enabled: Boolean = true,
    ) = SyncExclusionRule(
        pattern = pattern,
        exclusionClass = exclusionClass,
        target = target,
        matchType = SyncExclusionMatchType.SubtreeName,
        enabled = enabled,
    )
}
