package mega.privacy.android.app.presentation.editProfile

import java.io.File

/**
 * @param avatarFile to display avatar, it can be null
 * @param avatarColor extract color from bitmap, use default color if bitmap is null
 * @param avatarFileLastModified Avatar file last modified timestamp that will be updated when the avatar file changes.
 * @param firstName
 * @param lastName
 */
data class EditProfileState(
    val avatarFile: File? = null,
    val avatarColor: Int = 0,
    val avatarFileLastModified: Long = 0L,
    val firstName: String = "",
    val lastName: String = "",
)