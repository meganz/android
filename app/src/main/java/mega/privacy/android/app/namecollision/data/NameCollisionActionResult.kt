package mega.privacy.android.app.namecollision.data

/**
 * Data class for showing the result of the performed action after resolving a name collision.
 * The action can be an upload, a movement or a copy.
 *
 * @property message        Message to show as the action result.
 * @property isForeignNode  True if the parent node in which the action should be performed is a foreign node and is in over quota.
 * @property shouldFinish   True if should finish the activity because there are no more collisions, false otherwise.
 */
data class NameCollisionActionResult(
    val message: String,
    val isForeignNode: Boolean = false,
    val shouldFinish: Boolean
)