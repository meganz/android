package mega.privacy.android.feature.devicecenter.ui.renamedevice.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Rename Device Dialog
 *
 * @property errorMessage The Error Message displayed when an issue with renaming the Device occurs
 * @property renameSuccessfulEvent Notifies that the Device has been successfully renamed
 */
data class RenameDeviceState(
    @StringRes val errorMessage: Int? = null,
    val renameSuccessfulEvent: StateEvent = consumed,
)