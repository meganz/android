package mega.privacy.android.app.main.dialog.shares

/**
 * Remove all sharing contact ui state
 *
 * @property numberOfShareContact the number of share contact
 * @property numberOfShareFolder the number of share folder
 * @property isLoading whether is loading
 * @property message the message
 */
data class RemoveAllSharingContactUiState(
    val numberOfShareContact: Int = 0,
    val numberOfShareFolder: Int = 0,
    val isLoading: Boolean = false,
    val message: String? = null
)