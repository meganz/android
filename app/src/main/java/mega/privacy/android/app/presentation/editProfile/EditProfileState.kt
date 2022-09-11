package mega.privacy.android.app.presentation.editProfile

import java.io.File

/**
 * @param avatarFile to display avatar, it can be null
 * @param avatarColor extract color from bitmap, use default color if bitmap is null
 */
data class EditProfileState(val avatarFile: File? = null, val avatarColor: Int = 0)