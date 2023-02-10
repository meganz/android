package mega.privacy.android.app.presentation.fileinfo

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Represents the job in progress state
 * @param progressMessage the String resource representing the message to show the progress
 */
sealed class FileInfoJobInProgressState(
    @StringRes val progressMessage: Int?,
) {
    /**
     * The node is loading its properties
     */
    object InitialLoading : FileInfoJobInProgressState(null)

    /**
     * The node is being copied to another folder
     */
    object Copying : FileInfoJobInProgressState(
        progressMessage = R.string.context_copying
    )

    /**
     * the node is being moved to another folder
     */
    object Moving : FileInfoJobInProgressState(
        progressMessage = R.string.context_moving
    )

    /**
     * the node is being moved to the rubbish bin
     */
    object MovingToRubbishBin : FileInfoJobInProgressState(
        progressMessage = R.string.context_move_to_trash
    )

    /**
     * the node is being deleted
     */
    object Deleting : FileInfoJobInProgressState(
        R.string.context_delete_from_mega
    )
}