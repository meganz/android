package mega.privacy.android.feature.sync.ui.renamebackup.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Rename and Create Backup Dialog
 *
 * @property errorMessage The Error Message displayed when an issue with renaming and creating the Backup occurs
 * @property successEvent Notifies that the Backup has been successfully renamed and created
 */
data class RenameAndCreateBackupState(
    @StringRes val errorMessage: Int? = null,
    val successEvent: StateEvent = consumed,
)