package mega.privacy.android.shared.nodes.namecollision

import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult

/**
 * What became of the action the user took on a collision. A copy or movement session raises
 * [ActionCompleted] or [Failed]; a folder-upload session raises [Resolved] once every collision has
 * a choice. Uploads are not outcomes: they are handed to the transfer handler through
 * [NameCollisionUiState.uploadEvent].
 */
sealed interface NameCollisionOutcome {

    /**
     * A copy or movement finished.
     *
     * @property result The message to show and whether the screen is done.
     */
    data class ActionCompleted(val result: NameCollisionActionResult) : NameCollisionOutcome

    /**
     * Every collision of a folder upload has a choice; the caller performs the upload.
     *
     * @property collisions The collisions with the user's choice set on each.
     */
    data class Resolved(val collisions: List<NodeNameCollisionResult>) : NameCollisionOutcome

    /**
     * A copy or movement failed.
     *
     * @property throwable The failure.
     */
    data class Failed(val throwable: Throwable) : NameCollisionOutcome
}
