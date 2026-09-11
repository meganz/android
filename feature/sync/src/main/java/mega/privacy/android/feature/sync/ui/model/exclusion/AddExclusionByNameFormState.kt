package mega.privacy.android.feature.sync.ui.model.exclusion

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as sharedR

/**
 * What the Add exclusion by-name form shows.
 *
 * @property errorRes the failure to show under the field, or `null` while the value is acceptable.
 * The caller decides it, because judging a value needs the rules already in the file
 */
internal data class AddExclusionByNameFormState(
    val typeOption: SyncExclusionTypeOption? = null,
    val condition: SyncExclusionCondition? = null,
    val value: String = "",
    @StringRes val errorRes: Int? = null,
) {

    /**
     * The line describing what the current type leaves out, or `null` under
     * [SyncExclusionTypeOption.Extensions], whose form supplies its own.
     */
    @get:StringRes
    val descriptionRes: Int?
        get() = when (typeOption) {
            null -> sharedR.string.sync_exclusions_add_description_initial
            SyncExclusionTypeOption.Files -> sharedR.string.sync_exclusions_add_description_files
            SyncExclusionTypeOption.Folders ->
                sharedR.string.sync_exclusions_add_description_folders

            SyncExclusionTypeOption.FilesAndFolders ->
                sharedR.string.sync_exclusions_add_description_files_and_folders

            SyncExclusionTypeOption.Extensions -> null
        }

    /**
     * Wildcards only mean anything under the one condition that reads them as such, and the field
     * shows one line at a time, so a failure takes the slot.
     */
    val showsWildcardHelper: Boolean
        get() = condition?.allowsWildcards == true && errorRes == null

    /** Extensions carry no condition, and nothing is matched before a type is picked. */
    val showsConditionChip: Boolean
        get() = typeOption != null && typeOption != SyncExclusionTypeOption.Extensions
}
