package mega.privacy.android.app.presentation.filelink.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Represents the job in progress state
 * @param progressMessage the String resource representing the message to show the progress
 */
sealed class FileLinkJobInProgressState(
    @StringRes val progressMessage: Int?,
) {
    /**
     * The node is loading its properties
     */
    object InitialLoading : FileLinkJobInProgressState(R.string.general_loading)

    /**
     * The node is being imported to another folder
     */
    object Importing : FileLinkJobInProgressState(progressMessage = R.string.general_importing)
}