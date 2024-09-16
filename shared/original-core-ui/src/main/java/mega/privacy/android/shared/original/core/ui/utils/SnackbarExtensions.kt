package mega.privacy.android.shared.original.core.ui.utils

import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar

/**
 * Shows or queues to be shown a [MegaSnackbar] at the bottom of the [Scaffold] at
 * which this state is attached and suspends until snackbar is disappeared.
 *
 * The duration will be set automatically depending on [message] length or [actionLabel]:
 *   - If the message is longer than 50 chars or if there's an action label: [SnackbarDuration.Long]
 *   - else: [SnackbarDuration.Short]
 *
 * [SnackbarHostState] guarantees to show at most one snackbar at a time. If this function is
 * called while another snackbar is already visible, it will be suspended until this snack
 * bar is shown and subsequently addressed. If the caller is cancelled, the snackbar will be
 * removed from display and/or the queue to be displayed.
 *
 *
 * @param message text to be shown in the Snackbar
 * @param actionLabel optional action label to show as button in the Snackbar
 *
 * @return [SnackbarResult.ActionPerformed] if option action has been clicked or
 * [SnackbarResult.Dismissed] if snackbar has been dismissed via timeout or by the user
 */
suspend fun SnackbarHostState.showAutoDurationSnackbar(
    message: String,
    actionLabel: String? = null,
) = this.showSnackbar(
    message = message,
    actionLabel = actionLabel,
    duration = if (message.length > 50 || actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
)