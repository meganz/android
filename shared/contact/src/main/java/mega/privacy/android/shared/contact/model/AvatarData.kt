package mega.privacy.android.shared.contact.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import java.io.File

/**
 * Avatar source. The two variants are mutually
 * exclusive: callers build whichever one they have data for.
 */
@Stable
sealed interface AvatarData {

    /** Photo avatar backed by an on-disk file.
     *
     * @property file File to render as the avatar.
     */
    data class Image(val file: File) : AvatarData

    /**
     * Fallback avatar: a coloured circle with the [initials] rendered on top.
     * Only the first character of [initials] is shown.
     *
     * @property initials Text to render on top of the avatar.
     * @property avatarColor Background color of the avatar.
     */
    data class Initials(val initials: String, val avatarColor: Color) : AvatarData
}