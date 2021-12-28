package mega.privacy.android.app.usecase.data

import mega.privacy.android.app.utils.Constants.INVALID_VALUE

/**
 * Data class containing all the info related to a movement request.
 *
 * @property isSingleAction     True if the movement is only of a node, false otherwise.
 * @property oldParentHandle    Handle of the old parent.
 * @property resultText         Text to show as result of the request, null if should not show anything.
 * @property isForeignNode      True if should show a foreign storage over quota warning, false otherwise.
 * @property isSuccess          True if all requests finished with success, false otherwise.
 */
data class MoveRequestResult(
    val isSingleAction: Boolean = false,
    val oldParentHandle: Long = INVALID_VALUE.toLong(),
    val resultText: String? = null,
    val isSuccess: Boolean = true,
    val isForeignNode: Boolean = false
)
