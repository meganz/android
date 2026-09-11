package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowDescription
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowUiItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import mega.privacy.android.shared.resources.R as sharedR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionRowMapperTest {

    private val underTest = SyncExclusionRowMapper(
        patternMapper = SyncExclusionPatternMapper(),
        supportingTextMapper = SyncExclusionSupportingTextMapper(),
    )

    @Test
    fun `test that the default set yields the five rows the design shows`() {
        val result = underTest(parsed(MegaIgnoreDefaults.RULES))

        assertThat(result.map { described(it).value })
            .containsExactly("Thumbs.db", "desktop.ini", "~", ".", "~.")
            .inOrder()
    }

    @Test
    fun `test that the default set leaves its extensions to the chips`() {
        val result = underTest(parsed(MegaIgnoreDefaults.RULES))

        assertThat(result).hasSize(5)
        assertThat(result.map { it.rule.pattern })
            .containsNoneOf("*.crdownload", "*.sb-????????-??????", "*.tmp")
    }

    @Test
    fun `test that a rule left to the chips still advances the rule index`() {
        val parsed = parsed(listOf(rule("*.tmp"), rule("draft*")))

        val result = underTest(parsed)

        assertThat(result.single().ruleIndex).isEqualTo(1)
    }

    @Test
    fun `test that the rule index counts rules and not every line`() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# a comment"),
                MegaIgnoreEntry.RuleEntry(rule("draft*")),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 1_024L),
                MegaIgnoreEntry.RuleEntry(rule("*report*")),
            ),
        )

        val result = underTest(parsed)

        assertThat(result.map { it.ruleIndex }).containsExactly(0, 1).inOrder()
    }

    @Test
    fun `test that the rule index resolves back to the rule the row shows`() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("# header"),
                MegaIgnoreEntry.RuleEntry(rule("Thumbs.db")),
                MegaIgnoreEntry.RuleEntry(rule("draft*", enabled = true)),
                MegaIgnoreEntry.RuleEntry(rule("draft*", enabled = false)),
            ),
        )

        val result = underTest(parsed)

        result.forEach { row ->
            assertThat(parsed.rules[row.ruleIndex]).isEqualTo(row.rule)
        }
    }

    @Test
    fun `test that two rules differing only by enabled get different identities`() {
        val parsed = parsed(
            listOf(
                rule("draft*", enabled = true),
                rule("draft*", enabled = false),
            ),
        )

        val result = underTest(parsed)

        assertThat(result.map { it.ruleIndex }).containsExactly(0, 1).inOrder()
        assertThat(result.map { it.rule.enabled }).containsExactly(true, false).inOrder()
    }

    @Test
    fun `test that a repeated identical rule still yields two distinct rows`() {
        val result = underTest(parsed(listOf(rule("draft*"), rule("draft*"))))

        assertThat(result.toSet()).hasSize(2)
    }

    @Test
    fun `test that supporting text follows the rule scope`() {
        val parsed = parsed(
            listOf(
                rule("draft*", target = SyncExclusionTarget.All),
                rule("draft*", target = SyncExclusionTarget.Directories),
                rule("draft*", target = SyncExclusionTarget.Files),
            ),
        )

        val result = underTest(parsed)

        assertThat(result.map { described(it).supportingTextRes }).containsExactly(
            sharedR.string.sync_exclusions_rule_files_and_folders_beginning_with,
            sharedR.string.sync_exclusions_rule_folders_beginning_with,
            sharedR.string.sync_exclusions_rule_files_beginning_with,
        ).inOrder()
    }

    @Test
    fun `test that an extension pattern on a narrower target is described as a row`() {
        val parsed = parsed(listOf(rule("*.tmp", target = SyncExclusionTarget.Directories)))

        val result = underTest(parsed)

        assertThat(described(result.single()).supportingTextRes)
            .isEqualTo(sharedR.string.sync_exclusions_rule_folders_ending_with)
        assertThat(described(result.single()).value).isEqualTo(".tmp")
    }

    @Test
    fun `test that an extension pattern on a re-include stays visible as a row`() {
        val parsed = parsed(
            listOf(rule("*.jpg", exclusionClass = SyncExclusionClass.Include)),
        )

        val result = underTest(parsed)

        assertThat(result).hasSize(1)
        assertThat(result.single().description)
            .isEqualTo(SyncExclusionRowDescription.Undescribed("*.jpg"))
    }

    @Test
    fun `test that an extension pattern on a symlink rule stays visible as a row`() {
        val parsed = parsed(listOf(rule("*.lnk", target = SyncExclusionTarget.Symlinks)))

        val result = underTest(parsed)

        assertThat(result.single().description)
            .isEqualTo(SyncExclusionRowDescription.Undescribed("*.lnk"))
    }

    @Test
    fun `test that an undescribed rule shows its pattern and not a fragment`() {
        val parsed = parsed(listOf(rule("draft*", target = SyncExclusionTarget.Symlinks)))

        val result = underTest(parsed)

        assertThat(result.single().description)
            .isEqualTo(SyncExclusionRowDescription.Undescribed("draft*"))
    }

    @Test
    fun `test that a re-include rule stays visible without copy`() {
        val parsed = parsed(
            listOf(rule("draft*", exclusionClass = SyncExclusionClass.Include)),
        )

        val result = underTest(parsed)

        assertThat(result.single().description)
            .isInstanceOf(SyncExclusionRowDescription.Undescribed::class.java)
    }

    @Test
    fun `test that lines holding no rule yield no rows`() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.PreservedEntry("+sync:.megaignore"),
                MegaIgnoreEntry.SizeSmallerEntry(bytes = 1L),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 2L),
            ),
        )

        assertThat(underTest(parsed)).isEmpty()
    }

    @Test
    fun `test that an empty file yields no rows`() {
        assertThat(underTest(ParsedMegaIgnore(entries = emptyList()))).isEmpty()
    }

    private fun described(item: SyncExclusionRowUiItem) =
        item.description as SyncExclusionRowDescription.Described

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
