package mega.privacy.android.app.presentation.node.dialogs.sharefolder.warning

import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * State for Share Folder Dialog
 * @property info info text of dialog
 * @property positiveButton positive button text of dialog
 * @property negativeButton cancel button text of dialog
 * @property typeNodeList list of TypedNode
 */
data class ShareFolderDialogState(
    @StringRes val info: Int? = null,
    @StringRes val positiveButton: Int = -1,
    @StringRes val negativeButton: Int? = null,
    val typeNodeList: List<TypedNode> = emptyList()
)