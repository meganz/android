package mega.privacy.android.app.presentation.node.dialogs.sharefolder.warning

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * State for Share Folder Dialog
 * @property info info text of dialog
 * @property positiveButton positive button text of dialog
 * @property negativeButton cancel button text of dialog
 */
data class ShareFolderDialogState(
    @StringRes val info: Int? = null,
    @StringRes val positiveButton: Int = -1,
    @StringRes val negativeButton: Int? = null,
)