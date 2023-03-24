package mega.privacy.android.domain.entity.mediaplayer

import java.io.Serializable

/**
 * Subtitle file info
 *
 * @property id subtitle file handle
 * @property name subtitle file name
 * @property url subtitle file url
 * @property parentName subtitle file parent name
 */
data class SubtitleFileInfo(
    val id: Long,
    val name: String,
    val url: String?,
    val parentName: String?,
) : Serializable