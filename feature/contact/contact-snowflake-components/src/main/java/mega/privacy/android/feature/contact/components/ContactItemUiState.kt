package mega.privacy.android.feature.contact.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import java.io.File

/**
 * UI state for [ContactItemView]. Pre-resolved presentational data for a
 * single contact row — no domain types, no Android resource lookups.
 *
 * @property displayName Resolved name to render as the row title.
 * @property statusText Pre-resolved subtitle (status label or "Last seen …"); null hides the subtitle.
 * @property status Drives the inline status indicator next to the title.
 * @property avatar Avatar source: image file or coloured initials.
 * @property isVerified Whether to overlay the "verified contact" badge on the avatar.
 */
@Stable
data class ContactItemUiState(
    val displayName: String,
    val statusText: String?,
    val status: ContactItemStatus,
    val avatar: AvatarData,
    val isVerified: Boolean,
)

/**
 * Avatar source for [ContactItemUiState]. The two variants are mutually
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

/**
 * Presentational status enum decoupled from any domain type.
 * [Unknown] hides the inline status indicator entirely.
 */
enum class ContactItemStatus {
    /**
     * Online
     */
    Online,

    /**
     * Away
     */
    Away,

    /**
     * Busy
     */
    Busy,

    /**
     * Offline
     */
    Offline,

    /**
     * Unknown
     */
    Unknown,
}
