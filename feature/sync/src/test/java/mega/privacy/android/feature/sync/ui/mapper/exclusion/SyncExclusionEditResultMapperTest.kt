package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionEditResultMapperTest {

    private val patternMapper = SyncExclusionPatternMapper()
    private val underTest = SyncExclusionEditResultMapper(patternMapper)
    private val prefillMapper = SyncExclusionEditPrefillMapper(patternMapper)

    @Test
    fun `test that invoke describes no change when the form has no type`() {
        val actual = underTest(
            AddExclusionByNameFormState(
                condition = SyncExclusionCondition.Containing,
                value = "temp",
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke describes no change when the form has no condition`() {
        val actual = underTest(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                value = "temp",
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke describes no change when the value is blank`() {
        val actual = underTest(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.Containing,
                value = "  ",
            ),
        )

        assertThat(actual).isNull()
    }

    /**
     * Extensions names a mode of the Add form rather than a node type, so a form holding it
     * describes no target an edit could state.
     */
    @Test
    fun `test that invoke describes no change for the extensions type`() {
        val actual = underTest(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Extensions,
                condition = SyncExclusionCondition.Containing,
                value = "mp3",
            ),
        )

        assertThat(actual).isNull()
    }

    @ParameterizedTest(name = "{0} becomes {1}")
    @MethodSource("targets")
    fun `test that invoke states the target the user picked`(
        typeOption: SyncExclusionTypeOption,
        expected: SyncExclusionTarget,
    ) {
        val actual = underTest(
            AddExclusionByNameFormState(
                typeOption = typeOption,
                condition = SyncExclusionCondition.EndingWith,
                value = "tmp",
            ),
        )

        assertThat(actual).isEqualTo(SyncExclusionEdit(pattern = "*tmp", target = expected))
    }

    private fun targets(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncExclusionTypeOption.Files, SyncExclusionTarget.Files),
        Arguments.of(SyncExclusionTypeOption.Folders, SyncExclusionTarget.Directories),
        Arguments.of(SyncExclusionTypeOption.FilesAndFolders, SyncExclusionTarget.All),
    )

    /**
     * An edit that stated a field the form never showed would silently drop the grammar a rule
     * read from another client's file carries.
     */
    @Test
    fun `test that invoke leaves every field the form does not render unstated`() {
        val actual = underTest(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.MatchingExactly,
                value = "notes.txt",
            ),
        )

        assertThat(actual).isNotNull()
        assertThat(actual?.exclusionClass).isNull()
        assertThat(actual?.matchType).isNull()
        assertThat(actual?.strategy).isNull()
        assertThat(actual?.enabled).isNull()
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("roundTrips")
    fun `test that invoke reproduces a rule opened and saved unchanged`(
        pattern: String,
        target: SyncExclusionTarget,
    ) {
        val original = SyncExclusionRule(
            pattern = pattern,
            target = target,
            matchType = SyncExclusionMatchType.SubtreeName,
        )

        val actual = underTest(prefillMapper(original))

        assertThat(actual?.pattern).isEqualTo(original.pattern)
        assertThat(actual?.target).isEqualTo(original.target)
    }

    private fun roundTrips(): Stream<Arguments> = SyncExclusionTarget.entries
        .filter { it != SyncExclusionTarget.Symlinks }
        .flatMap { target ->
            listOf("draft*", "*temp", "*cache*", "notes.txt", "a?b*c").map { pattern ->
                Arguments.of(pattern, target)
            }
        }
        .stream()
}
