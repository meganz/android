package mega.privacy.android.feature.sync.ui.model.exclusion

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The label shown for this option in the Type picker, and on the chip once it is chosen.
 */
@get:StringRes
internal val SyncExclusionTypeOption.labelRes: Int
    get() = when (this) {
        SyncExclusionTypeOption.Files -> sharedR.string.sync_exclusions_add_type_files
        SyncExclusionTypeOption.Folders -> sharedR.string.sync_exclusions_add_type_folders
        SyncExclusionTypeOption.FilesAndFolders ->
            sharedR.string.sync_exclusions_add_type_files_and_folders

        SyncExclusionTypeOption.Extensions -> sharedR.string.sync_exclusions_add_type_extensions
    }

/**
 * The label shown for this condition in the Condition picker, and on the chip once it is chosen.
 *
 * Sentence case is held here rather than lower-cased at use, which would break languages that
 * capitalise differently from English.
 */
@get:StringRes
internal val SyncExclusionCondition.labelRes: Int
    get() = when (this) {
        SyncExclusionCondition.BeginningWith ->
            sharedR.string.sync_exclusions_add_condition_beginning_with

        SyncExclusionCondition.Containing -> sharedR.string.sync_exclusions_add_condition_containing
        SyncExclusionCondition.EndingWith ->
            sharedR.string.sync_exclusions_add_condition_ending_with

        SyncExclusionCondition.MatchingExactly ->
            sharedR.string.sync_exclusions_add_condition_matching_exactly

        SyncExclusionCondition.MatchingWildcard ->
            sharedR.string.sync_exclusions_add_condition_matching_wildcard
    }
