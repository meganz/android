package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.utils.LocationInfo
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * get the [LocationInfo] of a [TypedNode]
 */
fun interface GetNodeLocationInfo {
    /**
     * @param typedNode the [TypedNode] we want to know the LocationInfo
     */
    suspend operator fun invoke(typedNode: TypedNode): LocationInfo?
}