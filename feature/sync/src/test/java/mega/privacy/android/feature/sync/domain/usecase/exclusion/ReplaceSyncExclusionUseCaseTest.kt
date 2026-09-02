package mega.privacy.android.feature.sync.domain.usecase.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
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

/**
 * Each preservation case changes nothing but the pattern and demands the rest of the rule back
 * field for field, since an edit can only be trusted for the fields it supplies.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReplaceSyncExclusionUseCaseTest {

    private val underTest = ReplaceSyncExclusionUseCase()

    @Test
    fun `test that replace keeps an include rule re-including when only the pattern changes`() {
        val source = rule(pattern = "Keep*", exclusionClass = SyncExclusionClass.Include)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(pattern = "Kept*"),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source.copy(pattern = "Kept*"))
    }

    @Test
    fun `test that replace keeps a case sensitive rule case sensitive when only the pattern changes`() {
        val source = rule(pattern = "Foo", strategy = SyncMatchStrategy.GlobCaseSensitive)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(pattern = "Bar"),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source.copy(pattern = "Bar"))
    }

    @Test
    fun `test that replace keeps a local name rule uninherited when only the pattern changes`() {
        val source = rule(pattern = "Foo", matchType = SyncExclusionMatchType.LocalName)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(pattern = "Bar"),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source.copy(pattern = "Bar"))
    }

    @Test
    fun `test that replace keeps a symlink rule targeting symlinks when only the pattern changes`() {
        val source = rule(pattern = "link", target = SyncExclusionTarget.Symlinks)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(pattern = "other-link"),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source.copy(pattern = "other-link"))
    }

    @Test
    fun `test that replace keeps a symlink rule targeting symlinks when the edit targets all node types`() {
        val source = rule(pattern = "link", target = SyncExclusionTarget.Symlinks)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(
                pattern = "link",
                target = SyncExclusionTarget.All,
            ),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source)
    }

    @Test
    fun `test that replace accepts an edit repeating a symlink rule's own target`() {
        val source = rule(pattern = "link", target = SyncExclusionTarget.Symlinks)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(
                pattern = "other-link",
                target = SyncExclusionTarget.Symlinks,
            ),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source.copy(pattern = "other-link"))
    }

    @Test
    fun `test that replace fails with an unsupported target when the edit moves a rule onto symlinks`() {
        val source = rule(pattern = "Foo", target = SyncExclusionTarget.All)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(target = SyncExclusionTarget.Symlinks),
        )

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.UnsupportedTarget::class.java)
        assertThat((error as SyncExclusionEditError.UnsupportedTarget).target)
            .isEqualTo(SyncExclusionTarget.Symlinks)
    }

    @Test
    fun `test that replace keeps a disabled rule disabled when only the pattern changes`() {
        val source = rule(pattern = "Foo", enabled = false)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(pattern = "Bar"),
        )

        assertThat(result.getOrThrow().rules).containsExactly(source.copy(pattern = "Bar"))
    }

    @Test
    fun `test that replace retargets a rule that does not target symlinks`() {
        val source = rule(pattern = "Foo", target = SyncExclusionTarget.All)

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(target = SyncExclusionTarget.Directories),
        )

        assertThat(result.getOrThrow().rules)
            .containsExactly(source.copy(target = SyncExclusionTarget.Directories))
    }

    @Test
    fun `test that replace applies every field the edit supplies`() {
        val source = rule(
            pattern = "Foo",
            exclusionClass = SyncExclusionClass.Exclude,
            target = SyncExclusionTarget.Files,
            matchType = SyncExclusionMatchType.SubtreeName,
            strategy = SyncMatchStrategy.GlobCaseInsensitive,
            enabled = true,
        )

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(
                pattern = "Bar",
                exclusionClass = SyncExclusionClass.Include,
                target = SyncExclusionTarget.Directories,
                matchType = SyncExclusionMatchType.Path,
                strategy = SyncMatchStrategy.GlobCaseSensitive,
                enabled = false,
            ),
        )

        assertThat(result.getOrThrow().rules).containsExactly(
            SyncExclusionRule(
                pattern = "Bar",
                exclusionClass = SyncExclusionClass.Include,
                target = SyncExclusionTarget.Directories,
                matchType = SyncExclusionMatchType.Path,
                strategy = SyncMatchStrategy.GlobCaseSensitive,
                enabled = false,
            )
        )
    }

    @Test
    fun `test that replace keeps a pattern holding commas verbatim`() {
        val source = rule(pattern = "Foo")

        val result = underTest(
            current = fileOf(source),
            ruleIndex = 0,
            expected = source,
            edit = SyncExclusionEdit(pattern = "a,b,c"),
        )

        assertThat(result.getOrThrow().rules.single().pattern).isEqualTo("a,b,c")
    }

    @Test
    fun `test that replace leaves every non rule entry in position`() {
        val result = underTest(
            current = interleaved,
            ruleIndex = 1,
            expected = second,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        assertThat(result.getOrThrow().entries).containsExactly(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(second.copy(pattern = "Edited*")),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third),
            larger,
        ).inOrder()
    }

    @Test
    fun `test that replace writes back at the source rule entries index when preserved lines come first`() {
        val result = underTest(
            current = interleaved,
            ruleIndex = 2,
            expected = third,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        assertThat(result.getOrThrow().entries).containsExactly(
            note,
            MegaIgnoreEntry.RuleEntry(first),
            smaller,
            MegaIgnoreEntry.RuleEntry(second),
            blank,
            invalid,
            MegaIgnoreEntry.RuleEntry(third.copy(pattern = "Edited*")),
            larger,
        ).inOrder()
    }

    @Test
    fun `test that replace addresses the rule at the index rather than an equal rule elsewhere`() {
        val duplicated = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(first),
                note,
                MegaIgnoreEntry.RuleEntry(first),
            )
        )

        val result = underTest(
            current = duplicated,
            ruleIndex = 1,
            expected = first,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        assertThat(result.getOrThrow().entries).containsExactly(
            MegaIgnoreEntry.RuleEntry(first),
            note,
            MegaIgnoreEntry.RuleEntry(first.copy(pattern = "Edited*")),
        ).inOrder()
    }

    @Test
    fun `test that replace keeps the byte order mark flag`() {
        val result = underTest(
            current = interleaved.copy(hadBom = true),
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        assertThat(result.getOrThrow().hadBom).isTrue()
    }

    @ParameterizedTest(name = "rule index {1}")
    @MethodSource("provideUnresolvableTargets")
    fun `test that replace fails with a stale rule target when no rule sits at the index`(
        current: ParsedMegaIgnore,
        ruleIndex: Int,
    ) {
        val result = underTest(
            current = current,
            ruleIndex = ruleIndex,
            expected = first,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.StaleRuleTarget::class.java)
        assertThat((error as SyncExclusionEditError.StaleRuleTarget).ruleIndex).isEqualTo(ruleIndex)
    }

    @Test
    fun `test that replace fails with a stale rule target when a rule was inserted above the target`() {
        val inserted = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(rule(pattern = "Inserted*")),
                MegaIgnoreEntry.RuleEntry(first),
                MegaIgnoreEntry.RuleEntry(second),
                MegaIgnoreEntry.RuleEntry(third),
            )
        )

        val result = underTest(
            current = inserted,
            ruleIndex = 1,
            expected = second,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.StaleRuleTarget::class.java)
        assertThat((error as SyncExclusionEditError.StaleRuleTarget).ruleIndex).isEqualTo(1)
    }

    @Test
    fun `test that replace accepts an edit when the rule was switched off elsewhere`() {
        val result = underTest(
            current = fileOf(first.copy(enabled = false)),
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(pattern = "Edited*"),
        )

        assertThat(result.getOrThrow().rules)
            .containsExactly(first.copy(pattern = "Edited*", enabled = false))
    }

    @Test
    fun `test that replace fails with a stale rule target when it switches a rule already switched elsewhere`() {
        val result = underTest(
            current = fileOf(first.copy(enabled = false)),
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(enabled = true),
        )

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.StaleRuleTarget::class.java)
        assertThat((error as SyncExclusionEditError.StaleRuleTarget).ruleIndex).isEqualTo(0)
    }

    @Test
    fun `test that replace switches a rule off when the file still holds the state the caller read`() {
        val result = underTest(
            current = fileOf(first),
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(enabled = false),
        )

        assertThat(result.getOrThrow().rules).containsExactly(first.copy(enabled = false))
    }

    @Test
    fun `test that replace fails with a duplicate rule when the edit would equal another rule`() {
        val result = underTest(
            current = fileOf(first, second),
            ruleIndex = 1,
            expected = second,
            edit = SyncExclusionEdit(pattern = first.pattern),
        )

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.DuplicateRule::class.java)
        assertThat((error as SyncExclusionEditError.DuplicateRule).pattern)
            .isEqualTo(first.pattern)
    }

    @Test
    fun `test that replace fails with a duplicate rule when the rule it would equal is switched off`() {
        val result = underTest(
            current = fileOf(first.copy(enabled = false), second),
            ruleIndex = 1,
            expected = second,
            edit = SyncExclusionEdit(pattern = first.pattern),
        )

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SyncExclusionEditError.DuplicateRule::class.java)
        assertThat((error as SyncExclusionEditError.DuplicateRule).pattern)
            .isEqualTo(first.pattern)
    }

    @Test
    fun `test that replace switches off one of a pair of rules the file already repeated`() {
        val result = underTest(
            current = fileOf(first, first),
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(enabled = false),
        )

        assertThat(result.getOrThrow().rules)
            .containsExactly(first.copy(enabled = false), first)
            .inOrder()
    }

    @Test
    fun `test that replace switches on one of a pair of rules the file already repeated`() {
        val disabled = first.copy(enabled = false)

        val result = underTest(
            current = fileOf(disabled, disabled),
            ruleIndex = 1,
            expected = disabled,
            edit = SyncExclusionEdit(enabled = true),
        )

        assertThat(result.getOrThrow().rules)
            .containsExactly(disabled, first)
            .inOrder()
    }

    @Test
    fun `test that replace accepts an edit that leaves the rule as it was`() {
        val result = underTest(
            current = fileOf(first, second),
            ruleIndex = 0,
            expected = first,
            edit = SyncExclusionEdit(pattern = first.pattern),
        )

        assertThat(result.getOrThrow()).isEqualTo(fileOf(first, second))
    }

    @Test
    fun `test that replace accepts an edit equalling another rule that differs in match type`() {
        val other = first.copy(matchType = SyncExclusionMatchType.LocalName)

        val result = underTest(
            current = fileOf(other, second),
            ruleIndex = 1,
            expected = second,
            edit = SyncExclusionEdit(pattern = first.pattern),
        )

        assertThat(result.getOrThrow().rules)
            .containsExactly(other, second.copy(pattern = first.pattern))
            .inOrder()
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

    private fun fileOf(vararg rules: SyncExclusionRule) = ParsedMegaIgnore(
        entries = rules.map { MegaIgnoreEntry.RuleEntry(it) },
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
