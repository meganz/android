package mega.privacy.android.app.mediaplayer.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile

/**
 * The state for general media player
 *
 * @property accountType the account type
 * @property isBusinessAccountExpired if the business account is expired
 * @property isHiddenNodesOnboarded if the user has been onboarded with hidden nodes
 * @property hiddenNodeEnabled whether the hidden node is enabled
 * @property showHiddenItems whether to show hidden items
 * @property showMoveToTrashDialog whether to show the move to trash dialog
 * @property nodeToMoveToTrash the node handle to move to trash
 * @property nameCollisionEvent event to resolve a name collision after a copy, move or import
 * @property snackbarMessageEvent event to show a snackbar with the given string resource id
 * @property exceptionEvent event to handle an exception thrown by a node action
 * @property startChatFileOfflineDownloadEvent event to save a chat file for offline use
 * @property downloadFileLinkNodeEvent event to download the node of a file link
 */
data class MediaPlayerState(
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val showHiddenItems: Boolean? = null,
    val showMoveToTrashDialog: Boolean = false,
    val nodeToMoveToTrash: Long? = null,
    val nameCollisionEvent: StateEventWithContent<NameCollision> = consumed(),
    val snackbarMessageEvent: StateEventWithContent<Int> = consumed(),
    val exceptionEvent: StateEventWithContent<Throwable> = consumed(),
    val startChatFileOfflineDownloadEvent: StateEventWithContent<ChatFile> = consumed(),
    val downloadFileLinkNodeEvent: StateEventWithContent<TypedNode> = consumed(),
)
