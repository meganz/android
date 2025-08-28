package mega.privacy.android.core.nodecomponents.dialog.removeshare

/**
 * Remove all sharing contact ui state
 *
 * @property numberOfShareContact the number of share contact
 * @property numberOfShareFolder the number of share folder
 */
data class RemoveShareFolderState(
    val numberOfShareContact: Int = 0,
    val numberOfShareFolder: Int = 0,
)