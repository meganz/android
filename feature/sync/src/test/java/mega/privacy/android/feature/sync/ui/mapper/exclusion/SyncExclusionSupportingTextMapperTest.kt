package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import mega.privacy.android.shared.resources.R as sharedR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncExclusionSupportingTextMapperTest {

    private val underTest = SyncExclusionSupportingTextMapper()

    private val describedScopes = listOf(
        SyncExclusionTarget.All,
        SyncExclusionTarget.Directories,
        SyncExclusionTarget.Files,
    )

    @Test
    fun `test that every described scope and condition pair has copy`() {
        val resolved = describedScopes.flatMap { target ->
            SyncExclusionCondition.entries.map { condition ->
                underTest(SyncExclusionClass.Exclude, target, condition)
            }
        }

        assertThat(resolved).hasSize(15)
        assertThat(resolved.filterNotNull()).hasSize(15)
    }

    @Test
    fun `test that no two scope and condition pairs share a string`() {
        val resolved = describedScopes.flatMap { target ->
            SyncExclusionCondition.entries.map { condition ->
                underTest(SyncExclusionClass.Exclude, target, condition)
            }
        }

        assertThat(resolved.toSet()).hasSize(15)
    }

    @Test
    fun `test that files and folders conditions map to their own strings`() {
        val target = SyncExclusionTarget.All

        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.BeginningWith))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_and_folders_beginning_with)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.Containing))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_and_folders_containing)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.EndingWith))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_and_folders_ending_with)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.MatchingExactly))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_and_folders_matching_exactly)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.MatchingWildcard))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_and_folders_matching_wildcard)
    }

    @Test
    fun `test that folders conditions map to their own strings`() {
        val target = SyncExclusionTarget.Directories

        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.BeginningWith))
            .isEqualTo(sharedR.string.sync_exclusions_rule_folders_beginning_with)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.Containing))
            .isEqualTo(sharedR.string.sync_exclusions_rule_folders_containing)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.EndingWith))
            .isEqualTo(sharedR.string.sync_exclusions_rule_folders_ending_with)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.MatchingExactly))
            .isEqualTo(sharedR.string.sync_exclusions_rule_folders_matching_exactly)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.MatchingWildcard))
            .isEqualTo(sharedR.string.sync_exclusions_rule_folders_matching_wildcard)
    }

    @Test
    fun `test that files conditions map to their own strings`() {
        val target = SyncExclusionTarget.Files

        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.BeginningWith))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_beginning_with)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.Containing))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_containing)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.EndingWith))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_ending_with)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.MatchingExactly))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_matching_exactly)
        assertThat(underTest(SyncExclusionClass.Exclude, target, SyncExclusionCondition.MatchingWildcard))
            .isEqualTo(sharedR.string.sync_exclusions_rule_files_matching_wildcard)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SyncExclusionCondition::class)
    fun `test that a symlink rule has no copy`(condition: SyncExclusionCondition) {
        val result = underTest(
            SyncExclusionClass.Exclude,
            SyncExclusionTarget.Symlinks,
            condition,
        )

        assertThat(result).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SyncExclusionTarget::class)
    fun `test that a re-include rule has no copy whatever it targets`(
        target: SyncExclusionTarget,
    ) {
        val resolved = SyncExclusionCondition.entries.map { condition ->
            underTest(SyncExclusionClass.Include, target, condition)
        }

        assertThat(resolved.filterNotNull()).isEmpty()
    }
}
