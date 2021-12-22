package mega.privacy.android.app.usecase.data

import mega.privacy.android.app.utils.Constants.INVALID_VALUE

/**
 * Data class containing all the info related to a movement request.
 *
 * @property singleAction   True if the movement is only of a node, false otherwise.
 * @property oldParent      Handle of the old parent.
 * @property newParent      Handle of the new parent.
 * @property resultText     Text to show as result of the request, null if should not show anything.
 * @property isForeignNode  True if should show a foreign storage over quota warning, false otherwise.
 * @property allSuccess     True if all requests finished with success, false otherwise.
 */
data class MoveActionResult(
    val singleAction: Boolean = false,
    val oldParent: Long = INVALID_VALUE.toLong(),
    val newParent: Long = INVALID_VALUE.toLong(),
    val resultText: String? = null,
    val allSuccess: Boolean = true,
    val isForeignNode: Boolean = false
)
