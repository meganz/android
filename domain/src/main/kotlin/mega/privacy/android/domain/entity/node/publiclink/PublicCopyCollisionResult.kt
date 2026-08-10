package mega.privacy.android.domain.entity.node.publiclink

/**
 * Pairs a [PublicNodeNameCollisionResult] with the target folder handle the user
 * picked, so the public-link copy can be resumed after the collision dialog.
 */
data class PublicCopyCollisionResult(
    val result: PublicNodeNameCollisionResult,
    val targetHandle: Long,
)