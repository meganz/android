package mega.privacy.android.app.mediaplayer.trackinfo

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.LocationInfo
import java.io.File

/**
 * This class hold UI info for an audio node.
 *
 * @property thumbnail the thumbnail of this node
 * @property availableOffline if this node is available in offline
 * @property size the human readable size of this node
 * @property location the human readable location of this node
 * @property added the human readable added time of this node
 * @property lastModified the human readable last modified time of this node
 * @property durationString the duration string of the audio
 * @property offlineRemovedEvent file removed from offline event
 * @property metadata the metadata of the audio
 * @property transferTriggerEvent transfer trigger state event
 */
data class TrackInfoState(
    val thumbnail: File? = null,
    val availableOffline: Boolean = false,
    val size: String = "",
    val location: LocationInfo? = null,
    val added: Long = 0,
    val lastModified: Long = 0,
    val durationString: String = "",
    val offlineRemovedEvent: StateEvent = consumed,
    val metadata: Metadata? = null,
    val transferTriggerEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)
