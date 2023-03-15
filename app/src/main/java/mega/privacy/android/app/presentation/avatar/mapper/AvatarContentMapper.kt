package mega.privacy.android.app.presentation.avatar.mapper

import androidx.compose.ui.unit.TextUnit
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import java.io.File

/**
 * Mapper that maps full name to [AvatarContent] that contains avatar information.
 */
fun interface AvatarContentMapper {
    /**
     * Invoke
     * @param fullName full name of user
     * @param localFile file of avatar photo
     * @return [AvatarContent] that contains the mapped avatar information.
     */
    suspend operator fun invoke(
        fullName: String?,
        localFile: File?,
        showBorder: Boolean,
        textSize: TextUnit,
        backgroundColor: suspend () -> Int,
    ): AvatarContent
}
