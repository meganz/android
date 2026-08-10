package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Get the [LocationInfo] of a [TypedNode].
 *
 * Returns a human readable description of where the node lives in the user account
 * (Cloud drive, Rubbish bin, Backups, Incoming shares, or a nested folder within those),
 * along with the parent handle metadata required by callers that navigate into the folder.
 */
fun interface GetNodeLocationInfo {
    /**
     * @param typedNode the [TypedNode] we want to know the LocationInfo for.
     * @return the resolved [LocationInfo], or null if the node has no root ancestor.
     */
    suspend operator fun invoke(typedNode: TypedNode): LocationInfo?
}