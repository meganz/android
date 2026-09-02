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
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveSyncExclusionUseCaseTest {

    private val underTest = RemoveSyncExclusionUseCase()

    @Test
    fun `test that remove drops the addressed rule and leaves every non rule entry in position`() {
        val result = underTest(current = interleaved, ruleIndex = 1, expected = second)

        assertThat(result.getOrThrow().entries).containsExactly(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
        ).inOrder()
    }

    @Test
    fun `test that remove addresses a rule that sits after preserved lines`() {
        val result = underTest(current = interleaved, ruleIndex = 2, expected = third)

        assertThat(result.getOrThrow().entries).containsExactly(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(second),
            blank,
            invalid,
            larger,
        ).inOrder()
    }

    @Test
    fun `test that remove drops the first rule without touching the note above it`() {
        val result = underTest(current = interleaved, ruleIndex = 0, expected = first)

        assertThat(result.getOrThrow().entries).containsExactly(
            note,
            smaller,
            MegaIgnoreEntry.RuleEntry(second),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
        ).inOrder()
    }

    @Test
    fun `test that remove drops only the rule at the index when an equal rule sits elsewhere`() {
        val duplicated = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(first),
                note,
                MegaIgnoreEntry.RuleEntry(first),
            )
        )

        val result = underTest(current = duplicated, ruleIndex = 1, expected = first)

        assertThat(result.getOrThrow().entries).containsExactly(
            MegaIgnoreEntry.RuleEntry(first),
            note,
        ).inOrder()
    }

    @Test
    fun `test that remove drops a disabled rule`() {
        val disabled = rule(pattern = "Off*", enabled = false)

        val result = underTest(
            current = ParsedMegaIgnore(entries = listOf(MegaIgnoreEntry.RuleEntry(disabled))),
            ruleIndex = 0,
            expected = disabled,
        )

        assertThat(result.getOrThrow().entries).isEmpty()
    }

    @Test
    fun `test that remove keeps the byte order mark flag`() {
        val result = underTest(
            current = interleaved.copy(hadBom = true),
            ruleIndex = 0,
            expected = first,
        )

        assertThat(result.getOrThrow().hadBom).isTrue()
    }

    @ParameterizedTest(name = "rule index {1}")
    @MethodSource("provideUnresolvableTargets")
    fun `test that remove fails with a stale rule target when no rule sits at the index`(
        current: ParsedMegaIgnore,
        ruleIndex: Int,
    ) {
        val result = underTest(current = current, ruleIndex = ruleIndex, expected = first)

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.StaleRuleTarget::class.java)
        assertThat((error as SyncExclusionEditError.StaleRuleTarget).ruleIndex).isEqualTo(ruleIndex)
    }

    @Test
    fun `test that remove fails with a stale rule target when a rule was inserted above the target`() {
        val inserted = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(rule(pattern = "Inserted*")),
                MegaIgnoreEntry.RuleEntry(first),
                MegaIgnoreEntry.RuleEntry(second),
                MegaIgnoreEntry.RuleEntry(third),
            )
        )

        val result = underTest(current = inserted, ruleIndex = 1, expected = second)

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.StaleRuleTarget::class.java)
        assertThat((error as SyncExclusionEditError.StaleRuleTarget).ruleIndex).isEqualTo(1)
    }

    @Test
    fun `test that remove drops the rule when it was switched off elsewhere`() {
        val result = underTest(
            current = ParsedMegaIgnore(
                entries = listOf(MegaIgnoreEntry.RuleEntry(first.copy(enabled = false))),
            ),
            ruleIndex = 0,
            expected = first,
        )

        assertThat(result.getOrThrow().entries).isEmpty()
    }

    private fun provideUnresolvableTargets(): Stream<Arguments> = Stream.of(
        Arguments.of(interleaved, -1),
        Arguments.of(interleaved, 3),
        Arguments.of(ParsedMegaIgnore(entries = listOf(note, smaller)), 0),
    )

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
