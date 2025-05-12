package mega.privacy.android.app.presentation.folderlink.model

/**
 * Sealed interface defining the error state of link
 *
 * @property NoError  No error
 * @property Unavailable  Folder link is unavailable
 * @property Expired  Folder link is expired
 */
sealed interface LinkErrorState {
    object NoError : LinkErrorState
    object Unavailable : LinkErrorState
    object Expired : LinkErrorState
}