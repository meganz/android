package mega.privacy.android.app.usecase.data

/**
 * Data class containing all the info related to a movement request.
 *
 * @property isSingleAction True if the removal is only of a node, false otherwise.
 * @property resultText     Text to show as result of the request, null if should not show anything.
 * @property isSuccess      True if all requests finished with success, false otherwise.
 */
data class RemoveRequestResult(
    val isSingleAction: Boolean = false,
    val resultText: String? = null,
    val isSuccess: Boolean = true
)
