package mega.privacy.android.app.presentation.folderlink.model

import androidx.annotation.StringRes

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel]
 *
 * @property isInitialState             Whether it is initial state
 * @property isLoginComplete            Whether is login is successfully completed
 * @property isNodesFetched             Whether nodes are fetched
 * @property askForDecryptionKeyDialog  Whether to show AskForDecryptionDialog
 * @property errorDialogTitle           String id of title for error dialog
 * @property errorDialogContent         String id of content for error dialog
 * @property snackBarMessage            String id of content for snack bar
 */
data class FolderLinkState(
    val isInitialState: Boolean = true,
    val isLoginComplete: Boolean = false,
    val isNodesFetched: Boolean = false,
    val askForDecryptionKeyDialog: Boolean = false,
    @StringRes val errorDialogTitle: Int = -1,
    @StringRes val errorDialogContent: Int = -1,
    @StringRes val snackBarMessage: Int = -1,
)