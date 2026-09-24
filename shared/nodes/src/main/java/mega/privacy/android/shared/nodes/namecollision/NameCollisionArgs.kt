package mega.privacy.android.shared.nodes.namecollision

import mega.privacy.android.domain.entity.node.NameCollision

/**
 * Launch source describing the collision(s) the [NameCollisionViewModel] has to resolve and the
 * context they were triggered from.
 *
 * @property isFolderUploadContext True when the collisions were triggered while uploading a folder,
 *                                 in which case the resolution is collected and forwarded in a single
 *                                 batch instead of being uploaded one by one.
 */
sealed interface NameCollisionArgs {

    val isFolderUploadContext: Boolean

    /**
     * Resolve a single name collision.
     *
     * @property collision The [NameCollision] to resolve.
     */
    data class Single(
        val collision: NameCollision,
        override val isFolderUploadContext: Boolean,
    ) : NameCollisionArgs

    /**
     * Resolve a list of name collisions.
     *
     * @property collisions The [NameCollision]s to resolve.
     */
    data class Multiple(
        val collisions: List<NameCollision>,
        override val isFolderUploadContext: Boolean,
    ) : NameCollisionArgs

    /**
     * The collision(s) could not be recovered, for example when the process was killed and the launch
     * source was lost. There is nothing to resolve, so the screen closes itself.
     */
    data object MissingLaunch : NameCollisionArgs {
        override val isFolderUploadContext: Boolean = false
    }
}
