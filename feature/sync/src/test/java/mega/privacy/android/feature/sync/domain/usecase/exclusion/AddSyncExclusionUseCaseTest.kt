package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddSyncExclusionUseCaseTest {

    private val underTest = AddSyncExclusionUseCase()

    @Test
    fun `test that add appends a genuinely new rule`() {
        val added = rule(pattern = "New*")

        val result = underTest(current = interleaved, rule = added)

        assertThat(result.entries).containsExactly(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(disabledSecond),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
            MegaIgnoreEntry.RuleEntry(added),
        ).inOrder()
    }

    @Test
    fun `test that add re-enables an existing disabled rule in place`() {
        val result = underTest(
            current = interleaved,
            rule = disabledSecond.copy(enabled = true),
        )

        assertThat(result.entries).containsExactly(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(disabledSecond.copy(enabled = true)),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
        ).inOrder()
    }

    @Test
    fun `test that add leaves the file untouched when an equal enabled rule already exists`() {
        val result = underTest(current = interleaved, rule = third)

        assertThat(result).isEqualTo(interleaved)
    }

    @Test
    fun `test that add re-enables the first equal rule rather than every one of them`() {
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(disabledSecond),
                note,
                MegaIgnoreEntry.RuleEntry(disabledSecond),
            )
        )

        val result = underTest(current = current, rule = disabledSecond.copy(enabled = true))

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.RuleEntry(disabledSecond.copy(enabled = true)),
            note,
            MegaIgnoreEntry.RuleEntry(disabledSecond),
        ).inOrder()
    }

    @Test
    fun `test that add appends a rule that differs only in case sensitivity`() {
        val added = first.copy(strategy = SyncMatchStrategy.GlobCaseSensitive)

        val result = underTest(current = interleaved, rule = added)

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.RuleEntry(added))
    }

    @Test
    fun `test that add appends a rule that differs only in match type`() {
        val added = first.copy(matchType = SyncExclusionMatchType.LocalName)

        val result = underTest(current = interleaved, rule = added)

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.RuleEntry(added))
    }

    @Test
    fun `test that add appends a rule that differs only in exclusion class`() {
        val added = first.copy(exclusionClass = SyncExclusionClass.Include)

        val result = underTest(current = interleaved, rule = added)

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.RuleEntry(added))
    }

    @Test
    fun `test that add appends a rule that differs only in target`() {
        val added = first.copy(target = SyncExclusionTarget.Directories)

        val result = underTest(current = interleaved, rule = added)

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.RuleEntry(added))
    }

    @Test
    fun `test that add keeps a pattern holding commas verbatim`() {
        val result = underTest(
            current = ParsedMegaIgnore(entries = emptyList()),
            rule = rule(pattern = "a,b,c"),
        )

        assertThat(result.rules.single().pattern).isEqualTo("a,b,c")
    }

    @Test
    fun `test that add honours enabled when it appends`() {
        val added = rule(pattern = "New*", enabled = false)

        val result = underTest(current = interleaved, rule = added)

        assertThat(result.entries.last()).isEqualTo(MegaIgnoreEntry.RuleEntry(added))
    }

    @Test
    fun `test that add switches an existing rule on even when handed it disabled`() {
        val result = underTest(current = interleaved, rule = disabledSecond)

        assertThat(result.rules).containsExactly(
            first,
            disabledSecond.copy(enabled = true),
            third,
        ).inOrder()
    }

    /**
     * Switching a rule back on leaves it where it sat, so a later re-include still overrides it and
     * the list shows the exclusion as on while the subtree keeps syncing.
     */
    @Test
    fun `test that add re-enables in place even when a later rule overrides the result`() {
        val exclude = rule(pattern = "Temp*", enabled = false)
        val include = rule(pattern = "Temp*", exclusionClass = SyncExclusionClass.Include)
        val current = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(exclude),
                MegaIgnoreEntry.RuleEntry(include),
            )
        )

        val result = underTest(current = current, rule = exclude.copy(enabled = true))

        assertThat(result.entries).containsExactly(
            MegaIgnoreEntry.RuleEntry(exclude.copy(enabled = true)),
            MegaIgnoreEntry.RuleEntry(include),
        ).inOrder()
    }

    @Test
    fun `test that add keeps the byte order mark flag`() {
        val result = underTest(
            current = interleaved.copy(hadBom = true),
            rule = rule(pattern = "New*"),
        )

        assertThat(result.hadBom).isTrue()
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
    private val disabledSecond = rule(pattern = "Second*", enabled = false)
    private val third = rule(pattern = "Third*")

    private val interleaved = ParsedMegaIgnore(
        entries = listOf(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(disabledSecond),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
        )
    )

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
}
