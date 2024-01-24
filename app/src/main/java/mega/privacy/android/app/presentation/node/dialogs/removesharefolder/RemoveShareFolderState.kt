package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Remove all sharing contact ui state
 *
 * @property numberOfShareContact the number of share contact
 * @property numberOfShareFolder the number of share folder
 * @property removeFolderShareEvent event for folder share
 */
data class RemoveShareFolderState(
    val numberOfShareContact: Int = 0,
    val numberOfShareFolder: Int = 0,
)