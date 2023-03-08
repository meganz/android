package mega.privacy.android.domain.entity.mediaplayer

/**
 * Subtitle file info
 *
 * @property name subtitle file name
 * @property url subtitle file url
 * @property path subtitle file path
 */
data class SubtitleFileInfo(
    val name: String,
    val url: String?,
    val path: String?,
)