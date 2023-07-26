package mega.privacy.android.feature.devicecenter.ui.renamedevice.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Rename Device Dialog
 *
 * @property renameSuccessfulEvent Notifies that the Device has been successfully renamed
 */
data class RenameDeviceState(
    val renameSuccessfulEvent: StateEvent = consumed,
)