package mega.privacy.android.app.presentation.editProfile

import java.io.File

/**
 * @property avatarFile to display avatar, it can be null
 * @property avatarColor extract color from bitmap, use default color if bitmap is null
 * @property avatarFileLastModified Avatar file last modified timestamp that will be updated when the avatar file changes.
 * @property firstName
 * @property lastName
 * @property offlineFilesExist
 * @property transfersExist
 */
data class EditProfileState(
    val avatarFile: File? = null,
    val avatarColor: Int = 0,
    val avatarFileLastModified: Long = 0L,
    val firstName: String = "",
    val lastName: String = "",
    val offlineFilesExist: Boolean = false,
    val transfersExist: Boolean = false,
)