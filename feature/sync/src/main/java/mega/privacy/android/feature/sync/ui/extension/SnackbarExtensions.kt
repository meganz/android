package mega.privacy.android.feature.sync.ui.extension

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * M3 counterpart of original-core-ui's `showAutoDurationSnackbar`, which is written against the
 * Material 2 [androidx.compose.material.SnackbarHostState] and so cannot be used from a screen
 * hosted by core-ui's `MegaScaffold`.
 *
 * The duration is derived from the content, as it was before: [SnackbarDuration.Long] when the
 * message runs past 50 characters or carries an action, [SnackbarDuration.Short] otherwise.
 *
 * @param message text to show in the snackbar
 * @param actionLabel optional action label shown as a button
 *
 * @return [SnackbarResult.ActionPerformed] if the action was clicked, or
 * [SnackbarResult.Dismissed] if it timed out or was dismissed by the user
 */
internal suspend fun SnackbarHostState.showAutoDurationSnackbar(
    message: String,
    actionLabel: String? = null,
) = showSnackbar(
    message = message,
    actionLabel = actionLabel,
    duration = if (message.length > 50 || actionLabel != null) {
        SnackbarDuration.Long
    } else {
        SnackbarDuration.Short
    },
)
