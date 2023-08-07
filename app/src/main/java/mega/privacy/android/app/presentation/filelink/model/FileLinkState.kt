package mega.privacy.android.app.presentation.filelink.model

import android.content.Intent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.PublicNodeException

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.filelink.FileLinkActivity]
 *
 * @property shouldLogin            Whether to show login screen
 * @property url                    Url of the file
 * @property hasDbCredentials       Whether has db credentials
 * @property title                  Title of the current node
 * @property sizeInBytes            Size of the current file node
 * @property handle                 Handle of the current file node
 * @property previewPath            Path of the preview image
 * @property serializedData         Serialized data of the file node
 * @property iconResource           the icon resource that represents this node
 * @property askForDecryptionDialog Whether to show AskForDecryptionDialog
 * @property collision              Node with existing names
 * @property copyThrowable          Throwable error on copy
 * @property collisionCheckThrowable Throwable error on collision check
 * @property copySuccess            Whether copy was success or not
 * @property fetchPublicNodeError   Exception while fetching current public node
 * @property jobInProgressState     indicates if there are any job in progress that needs to be notified
 * @property openFile               State to handle file opening
 */
data class FileLinkState(
    val shouldLogin: Boolean? = null,
    val hasDbCredentials: Boolean = false,
    val url: String = "",
    val title: String = "",
    val sizeInBytes: Long = 0,
    val handle: Long = -1,
    val previewPath: String? = null,
    val serializedData: String? = null,
    val iconResource: Int? = null,
    val askForDecryptionDialog: Boolean = false,
    val collision: NameCollision? = null,
    val collisionCheckThrowable: Throwable? = null,
    val copyThrowable: Throwable? = null,
    val copySuccess: Boolean = false,
    val fetchPublicNodeError: PublicNodeException? = null,
    val jobInProgressState: FileLinkJobInProgressState? = FileLinkJobInProgressState.InitialLoading,
    val openFile: StateEventWithContent<Intent> = consumed(),
) {
    /**
     * Creates a copy of this view state with the info that can be extracted directly from typedNode
     */
    fun copyWithTypedNode(typedNode: TypedFileNode, iconResource: Int) = this.copy(
        title = typedNode.name,
        sizeInBytes = typedNode.size,
        previewPath = typedNode.previewPath,
        iconResource = if (typedNode.previewPath == null) iconResource else null,
        handle = typedNode.id.longValue,
        serializedData = typedNode.serializedData
    )
}
