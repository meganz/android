package mega.privacy.android.app.presentation.videosection.model

/**
 * Video playlist set ui entity
 *
 * @property id the id of entity
 * @property title the title od entity
 * @property isSelected the entity whether is selected
 */
data class VideoPlaylistSetUiEntity(
    val id: Long,
    val title: String,
    val isSelected: Boolean = false,
)
