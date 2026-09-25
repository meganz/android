package mega.privacy.android.feature.sync.ui.renamebackup.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Rename and Create Backup Dialog
 *
 * @property errorMessage The Error Message displayed when an issue with renaming and creating the Backup occurs
 * @property successEvent Notifies that the Backup was renamed and handed to the SDK, carrying
 * whether the SDK reported it created
 */
internal data class RenameAndCreateBackupState(
    @StringRes val errorMessage: Int? = null,
    val successEvent: StateEventWithContent<Boolean> = consumed(),
)
