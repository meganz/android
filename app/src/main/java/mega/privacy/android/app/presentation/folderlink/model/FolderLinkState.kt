package mega.privacy.android.app.presentation.folderlink.model

import androidx.annotation.StringRes
import mega.privacy.android.app.namecollision.data.NameCollision

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel]
 *
 * @property isInitialState             Whether it is initial state
 * @property isLoginComplete            Whether is login is successfully completed
 * @property isNodesFetched             Whether nodes are fetched
 * @property askForDecryptionKeyDialog  Whether to show AskForDecryptionDialog
 * @property collisions                 List of nodes with existing names
 * @property copyResultText             Text to show on successful copy
 * @property copyThrowable              Throwable error on copy
 * @property errorDialogTitle           String id of title for error dialog
 * @property errorDialogContent         String id of content for error dialog
 * @property snackBarMessage            String id of content for snack bar
 */
data class FolderLinkState(
    val isInitialState: Boolean = true,
    val isLoginComplete: Boolean = false,
    val isNodesFetched: Boolean = false,
    val askForDecryptionKeyDialog: Boolean = false,
    val collisions: ArrayList<NameCollision>? = null,
    val copyThrowable: Throwable? = null,
    val copyResultText: String? = null,
    @StringRes val errorDialogTitle: Int = -1,
    @StringRes val errorDialogContent: Int = -1,
    @StringRes val snackBarMessage: Int = -1,
)