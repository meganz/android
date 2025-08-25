package mega.privacy.android.app.appstate.global.util

import androidx.compose.material3.SnackbarDuration as SnackbarDurationM3
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration

/**
 * Extension function to show a snackbar with the given attributes. This is a M3 version of the snackbar.
 * This function suspends until the snackbar is dismissed or the action is performed.
 *
 * @param attributes The attributes for the snackbar, including message, action label, duration, and action click handler.
 */
suspend fun SnackbarHostState.show(attributes: SnackbarAttributes) {
    val result = showSnackbar(
        message = attributes.message.orEmpty(),
        actionLabel = attributes.action,
        duration = when (attributes.duration) {
            SnackbarDuration.Short -> SnackbarDurationM3.Short
            SnackbarDuration.Long -> SnackbarDurationM3.Long
            SnackbarDuration.Indefinite -> SnackbarDurationM3.Indefinite
        },
        withDismissAction = attributes.withDismissAction,
    )

    if (result == SnackbarResult.ActionPerformed) {
        attributes.actionClick?.invoke()
    }
}