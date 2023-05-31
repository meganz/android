package mega.privacy.android.app.mediaplayer.model

import android.content.Intent

/**
 * The entity for media player menu clicked event
 *
 * @property menuId menu item id
 * @property adapterType the type of adapter
 * @property playingHandle current playing media time handle
 * @property launchIntent the launched Intent
 */
data class MediaPlayerMenuClickedEvent(
    val menuId: Int,
    val adapterType: Int,
    val playingHandle: Long,
    val launchIntent: Intent,
)