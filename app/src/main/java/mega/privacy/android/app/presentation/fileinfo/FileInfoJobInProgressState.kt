package mega.privacy.android.app.presentation.fileinfo

/**
 * Represents the job in progress state
 */
sealed interface FileInfoJobInProgressState {
    /**
     * The node is loading its properties
     */
    object InitialLoading : FileInfoJobInProgressState

    /**
     * The node is being copied to another folder
     */
    object Copying : FileInfoJobInProgressState

    /**
     * the node is being moved to another folder
     */
    object Moving : FileInfoJobInProgressState
}