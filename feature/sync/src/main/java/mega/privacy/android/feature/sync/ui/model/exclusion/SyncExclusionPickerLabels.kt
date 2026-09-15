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

/**
 * The label shown for this condition in the size picker, and on the chip once it is chosen.
 */
@get:StringRes
internal val SyncSizeCondition.labelRes: Int
    get() = when (this) {
        SyncSizeCondition.BiggerThan ->
            sharedR.string.sync_exclusions_add_size_condition_bigger_than

        SyncSizeCondition.SmallerThan ->
            sharedR.string.sync_exclusions_add_size_condition_smaller_than

        SyncSizeCondition.NotBetween ->
            sharedR.string.sync_exclusions_add_size_condition_not_between
    }

/**
 * The abbreviation shown for this unit in the unit picker, and in the field once it is chosen.
 *
 * The abbreviations are translated rather than taken from the enum name, which would leave every
 * locale reading the English one.
 */
@get:StringRes
internal val SyncSizeUnit.labelRes: Int
    get() = when (this) {
        SyncSizeUnit.Bytes -> sharedR.string.sync_exclusions_add_size_unit_bytes
        SyncSizeUnit.Kilobytes -> sharedR.string.sync_exclusions_add_size_unit_kilobytes
        SyncSizeUnit.Megabytes -> sharedR.string.sync_exclusions_add_size_unit_megabytes
        SyncSizeUnit.Gigabytes -> sharedR.string.sync_exclusions_add_size_unit_gigabytes
    }
