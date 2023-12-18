package mega.privacy.android.app.presentation.audiosection.model

import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * The entity for the audio is displayed in audios section
 *
 * @property id NodeId
 * @property name the audio's name
 * @property size the audio's size
 * @property duration the audio's duration
 * @property thumbnail the audio's thumbnail
 * @property isFavourite the audio if is Favourite
 * @property isExported the audio if is Exported
 * @property isTakenDown the audio if is TakenDown
 * @property hasVersions the audio if has Versions
 * @property modificationTime the audio's modification time
 * @property label the audio's label
 * @property nodeAvailableOffline the audio if is available for offline
 * @property isSelected the audio if is selected
 */
data class UIAudio(
    val id: NodeId,
    val name: String,
    val size: Long,
    val duration: String?,
    val thumbnail: File? = null,
    val isFavourite: Boolean = false,
    val isExported: Boolean = false,
    val isTakenDown: Boolean = false,
    val hasVersions: Boolean = false,
    val modificationTime: Long,
    val label: Int,
    val nodeAvailableOffline: Boolean = false,
    val isSelected: Boolean = false,
)
