package mega.privacy.android.shared.nodes.namecollision

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Ui State for Name Collision.
 *
 * @property currentCollision       Complete data of the collision currently being resolved, null while
 *                                  loading or once there are no more collisions to resolve.
 * @property fileVersioningInfo     Info required to present the replace, update or merge option of the
 *                                  current collision, null until it has been resolved.
 * @property isCopyToOrigin         True when the node is being copied into the folder it already belongs
 *                                  to, in which case it is duplicated without presenting any option.
 * @property pendingFileCollisions  Number of file collisions left to resolve after the current one.
 * @property pendingFolderCollisions Number of folder collisions left to resolve after the current one.
 * @property currentCollisionIndex  1-based position of the current collision among the collisions of
 *                                  its own type, files and folders being counted separately; 0 while
 *                                  nothing is presented.
 * @property currentTypeCollisionCount Number of collisions of the current collision's type in the
 *                                  batch; 0 while nothing is presented.
 * @property hasPendingCollisions   True while collisions are still queued behind the current one; false
 *                                  once an action has taken them all.
 * @property uploadedFilesCount     Number of files the user has chosen to upload so far, for the
 *                                  "upload started" message once the last collision is resolved.
 * @property uploadEvent            Event carrying the upload to be forwarded to the transfer handler.
 * @property outcome                Event carrying the terminal result of the user's action to return
 *                                  to the caller before the screen closes, see [NameCollisionOutcome].
 * @property dismissEvent           Event asking the screen to close without returning a result, raised
 *                                  when there is nothing to resolve or the user has skipped or uploaded
 *                                  the last collision.
 */
data class NameCollisionUiState(
    val currentCollision: NodeNameCollisionResult?,
    val fileVersioningInfo: FileVersioningInfo?,
    val isCopyToOrigin: Boolean,
    val pendingFileCollisions: Int,
    val pendingFolderCollisions: Int,
    val currentCollisionIndex: Int,
    val currentTypeCollisionCount: Int,
    val hasPendingCollisions: Boolean,
    val uploadedFilesCount: Int,
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload.CollidedFiles>,
    val outcome: StateEventWithContent<NameCollisionOutcome>,
    val dismissEvent: StateEvent,
)
