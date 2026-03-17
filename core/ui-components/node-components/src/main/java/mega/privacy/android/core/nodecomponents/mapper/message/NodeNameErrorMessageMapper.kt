package mega.privacy.android.core.nodecomponents.mapper.message

import androidx.annotation.StringRes
import mega.privacy.android.shard.nodes.R as NodesR
import mega.privacy.android.domain.entity.InvalidNameType
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Mapper implementation for Version History Remove Message
 *
 * @param context as Application Context provided by dependency graph
 */
class NodeNameErrorMessageMapper @Inject constructor() {
    /**
     * Invoke.
     */
    @StringRes
    operator fun invoke(invalidNameType: InvalidNameType, isFolder: Boolean): Int? =
        when (invalidNameType) {
            InvalidNameType.BLANK_NAME -> if (isFolder) {
                sharedR.string.create_new_folder_dialog_error_message_empty_folder_name
            } else {
                NodesR.string.invalid_string
            }

            InvalidNameType.INVALID_NAME -> sharedR.string.general_invalid_characters_defined
            InvalidNameType.NAME_ALREADY_EXISTS -> if (isFolder) {
                sharedR.string.create_new_folder_dialog_error_existing_folder
            } else {
                NodesR.string.same_file_name_warning
            }

            InvalidNameType.NO_EXTENSION -> NodesR.string.file_without_extension_warning
            InvalidNameType.DOT_NAME -> sharedR.string.general_invalid_dot_name_warning
            InvalidNameType.DOUBLE_DOT_NAME -> sharedR.string.general_invalid_double_dot_name_warning
            else -> null
        }
}