package mega.privacy.android.app.presentation.avatar.mapper

import mega.privacy.android.app.presentation.avatar.model.AvatarContent

/**
 * Mapper that maps full name to [AvatarContent] that contains avatar information.
 */
fun interface AvatarContentMapper {
    /**
     * Invoke
     * @param fullName full name of user
     * @return [AvatarContent] that contains the mapped avatar information.
     */
    suspend operator fun invoke(fullName: String?): AvatarContent
}
