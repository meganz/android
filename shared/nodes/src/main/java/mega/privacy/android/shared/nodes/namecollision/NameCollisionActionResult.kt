package mega.privacy.android.shared.nodes.namecollision

/**
 * Data class for showing the result of the performed action after resolving a name collision.
 * The action can be an upload, a movement or a copy.
 *
 * @property message        Message to show as the action result.
 * @property isForeignNode  True if the parent node in which the action should be performed is a foreign node and is in over quota.
 */
data class NameCollisionActionResult(
    val message: String? = null,
    val isForeignNode: Boolean = false,
)
