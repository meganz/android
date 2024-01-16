package mega.privacy.android.app.presentation.node.dialogs.sharefolder

import androidx.annotation.StringRes

/**
 * State for Share Folder Dialog
 * @property title title of dialog
 * @property info info text of dialog
 * @property positiveButton positive button text of dialog
 * @property negativeButton cancel button text of dialog
 * @property shouldHandlePositiveClick if click of positive button should be handled or not
 */
data class ShareFolderDialogState(
    @StringRes val title: Int = -1,
    @StringRes val info: Int = -1,
    @StringRes val positiveButton: Int = -1,
    @StringRes val negativeButton: Int? = null,
    val shouldHandlePositiveClick: Boolean = false
)