package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionEditPrefillMapperTest {

    private val patternMapper = SyncExclusionPatternMapper()
    private val underTest = SyncExclusionEditPrefillMapper(patternMapper)

    private fun rule(
        pattern: String,
        target: SyncExclusionTarget = SyncExclusionTarget.All,
        exclusionClass: SyncExclusionClass = SyncExclusionClass.Exclude,
    ) = SyncExclusionRule(
        pattern = pattern,
        exclusionClass = exclusionClass,
        target = target,
        matchType = SyncExclusionMatchType.SubtreeName,
    )

    @ParameterizedTest(name = "{0} prefills the {1} type")
    @MethodSource("targets")
    fun `test that invoke prefills the type the rule targets`(
        target: SyncExclusionTarget,
        expected: SyncExclusionTypeOption?,
    ) {
        val actual = underTest(rule(pattern = "temp*", target = target))

        assertThat(actual.typeOption).isEqualTo(expected)
    }

    private fun targets(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncExclusionTarget.All, SyncExclusionTypeOption.FilesAndFolders),
        Arguments.of(SyncExclusionTarget.Directories, SyncExclusionTypeOption.Folders),
        Arguments.of(SyncExclusionTarget.Files, SyncExclusionTypeOption.Files),
        Arguments.of(SyncExclusionTarget.Symlinks, null),
    )

    @ParameterizedTest(name = "{0} prefills {1} with {2}")
    @MethodSource("patterns")
    fun `test that invoke prefills the condition and value the pattern reads as`(
        pattern: String,
        expectedCondition: SyncExclusionCondition,
        expectedValue: String,
    ) {
        val actual = underTest(rule(pattern = pattern))

        assertThat(actual.condition).isEqualTo(expectedCondition)
        assertThat(actual.value).isEqualTo(expectedValue)
    }

    private fun patterns(): Stream<Arguments> = Stream.of(
        Arguments.of("temp*", SyncExclusionCondition.BeginningWith, "temp"),
        Arguments.of("*temp*", SyncExclusionCondition.Containing, "temp"),
        Arguments.of("*temp", SyncExclusionCondition.EndingWith, "temp"),
        Arguments.of("notes.txt", SyncExclusionCondition.MatchingExactly, "notes.txt"),
        Arguments.of("draft?.*", SyncExclusionCondition.MatchingWildcard, "draft?.*"),
    )

    @Test
    fun `test that invoke prefills an extension pattern as a wildcard carrying the whole pattern`() {
        val actual = underTest(rule(pattern = "*.mp3"))

        assertThat(actual.condition).isEqualTo(SyncExclusionCondition.MatchingWildcard)
        assertThat(actual.value).isEqualTo("*.mp3")
    }

    @Test
    fun `test that invoke prefills a re-include rule with no type and its whole pattern`() {
        val actual = underTest(
            rule(pattern = "*keep", exclusionClass = SyncExclusionClass.Include),
        )

        assertThat(actual.typeOption).isNull()
        assertThat(actual.condition).isEqualTo(SyncExclusionCondition.MatchingWildcard)
        assertThat(actual.value).isEqualTo("*keep")
    }

    @Test
    fun `test that invoke prefills a symlink rule with no type and its whole pattern`() {
        val actual = underTest(
            rule(pattern = "draft*", target = SyncExclusionTarget.Symlinks),
        )

        assertThat(actual.typeOption).isNull()
        assertThat(actual.condition).isEqualTo(SyncExclusionCondition.MatchingWildcard)
        assertThat(actual.value).isEqualTo("draft*")
    }

    @Test
    fun `test that invoke composes back to the pattern it prefilled from`() {
        val patterns = listOf(
            "temp*", "*temp*", "*temp", "notes.txt", "draft?.*", "*.mp3",
            "*", "**", "a", "a/b", "temp ",
        )
        val rules = patterns.flatMap { pattern ->
            listOf(
                rule(pattern = pattern),
                rule(pattern = pattern, target = SyncExclusionTarget.Symlinks),
                rule(pattern = pattern, exclusionClass = SyncExclusionClass.Include),
            )
        }

        rules.forEach { rule ->
            val prefilled = underTest(rule)
            val composed = patternMapper(
                condition = checkNotNull(prefilled.condition),
                value = prefilled.value,
            )

            assertThat(composed).isEqualTo(rule.pattern)
        }
    }

    @Test
    fun `test that invoke leaves the field empty of an error`() {
        val actual = underTest(rule(pattern = "temp*"))

        assertThat(actual.errorRes).isNull()
    }
}
