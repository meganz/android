package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Get the description for this permission, Unknown permission has no description as it should not be shown, so null is returned in this case
 * @return the String resource [Int] for the description for this [AccessPermission] or null if it has no description
 */
fun AccessPermission.description() = when (this) {
    AccessPermission.OWNER, AccessPermission.FULL -> R.string.file_properties_shared_folder_full_access
    AccessPermission.READ -> R.string.file_properties_shared_folder_read_only
    AccessPermission.READWRITE -> R.string.file_properties_shared_folder_read_write
    AccessPermission.UNKNOWN -> null
}