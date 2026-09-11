package mega.privacy.android.feature.sync.ui.model.exclusion

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import mega.privacy.android.shared.resources.R as sharedR

internal class AddExclusionByNameFormStateTest {

    @Test
    fun `test that descriptionRes prompts for a choice when no type is picked`() {
        val underTest = AddExclusionByNameFormState()

        assertThat(underTest.descriptionRes)
            .isEqualTo(sharedR.string.sync_exclusions_add_description_initial)
    }

    @Test
    fun `test that descriptionRes describes files when the files type is picked`() {
        val underTest = AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files)

        assertThat(underTest.descriptionRes)
            .isEqualTo(sharedR.string.sync_exclusions_add_description_files)
    }

    @Test
    fun `test that descriptionRes describes folders when the folders type is picked`() {
        val underTest = AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Folders)

        assertThat(underTest.descriptionRes)
            .isEqualTo(sharedR.string.sync_exclusions_add_description_folders)
    }

    @Test
    fun `test that descriptionRes describes both when the files and folders type is picked`() {
        val underTest =
            AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.FilesAndFolders)

        assertThat(underTest.descriptionRes)
            .isEqualTo(sharedR.string.sync_exclusions_add_description_files_and_folders)
    }

    @Test
    fun `test that descriptionRes defers when the extensions type is picked`() {
        val underTest = AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Extensions)

        assertThat(underTest.descriptionRes).isNull()
    }

    @Test
    fun `test that showsWildcardHelper is false for a condition that reads no wildcards`() {
        val underTest = AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.Files,
            condition = SyncExclusionCondition.BeginningWith,
        )

        assertThat(underTest.showsWildcardHelper).isFalse()
    }

    @Test
    fun `test that showsWildcardHelper is true for the matching wildcard condition`() {
        val underTest = AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.Files,
            condition = SyncExclusionCondition.MatchingWildcard,
        )

        assertThat(underTest.showsWildcardHelper).isTrue()
    }

    @Test
    fun `test that showsWildcardHelper is false when an error takes the slot`() {
        val underTest = AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.Files,
            condition = SyncExclusionCondition.MatchingWildcard,
            errorRes = sharedR.string.sync_exclusions_add_name_duplicate_error,
        )

        assertThat(underTest.showsWildcardHelper).isFalse()
    }

    @Test
    fun `test that showsConditionChip is false when no type is picked`() {
        assertThat(AddExclusionByNameFormState().showsConditionChip).isFalse()
    }

    @Test
    fun `test that showsConditionChip is false when the extensions type is picked`() {
        val underTest = AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Extensions)

        assertThat(underTest.showsConditionChip).isFalse()
    }

    @Test
    fun `test that showsConditionChip is true when a name type is picked`() {
        val underTest = AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Folders)

        assertThat(underTest.showsConditionChip).isTrue()
    }
}
