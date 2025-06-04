package mega.privacy.android.app.utils.wrapper

import mega.privacy.android.domain.entity.node.TypedNode
import nz.mega.sdk.MegaNode

/**
 * Wrapper class for MegaNode and TypedNode - Use to replace MegaNode in Viewmodels/Activities that still use MegaNode due to dependencies on the presentation side
 *
 * @property node MegaNode instance
 * @property typedNode TypedNode instance
 */
data class LegacyNodeWrapper(
    val node: MegaNode,
    val typedNode: TypedNode,
)