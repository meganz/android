package mega.privacy.android.feature.sync.ui.createnewfolder.model

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Data class representing the state of the Create New Folder Dialog
 *
 * @property errorMessage The Error Message displayed when occurs an issue creating the folder
 * @property validNameConfirmed Notifies that the new folder name is valid
 */
internal data class CreateNewFolderState(
    @StringRes val errorMessage: Int? = null,
    val validNameConfirmed: StateEventWithContent<String> = consumed(),
)