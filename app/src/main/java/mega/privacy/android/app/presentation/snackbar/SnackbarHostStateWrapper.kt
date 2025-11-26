package mega.privacy.android.app.presentation.snackbar

import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration as SnackbarDurationM2
import androidx.compose.material.SnackbarHostState as SnackbarHostStateM2
import androidx.compose.material.SnackbarResult
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.app.appstate.global.util.toM3Duration
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import timber.log.Timber

/**
 * SnackbarHostState wrapper to have a common interface to show snackbar messages with both Material2 and Material3.
 * It's just a temporary solution while migrating to Material3. It will be removed once everything is migrated to Material3
 */
class SnackbarHostStateWrapper private constructor(
    private val snackBarHostStateM2: SnackbarHostStateM2?,
    private val snackBarHostState: SnackbarHostState?,
) {
    constructor(snackBarHostStateM2: SnackbarHostStateM2) : this(snackBarHostStateM2, null)
    constructor(snackBarHostState: SnackbarHostState)
            : this(null, snackBarHostState)

    /**
     * Shows or queues to be shown a [Snackbar] at the bottom of the [Scaffold] to which this state
     * is attached and suspends until the snackbar has disappeared.
     */
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDurationM2 = SnackbarDurationM2.Short,
    ) = snackBarHostStateM2?.showSnackbar(message, actionLabel, duration)
        ?: snackBarHostState?.showSnackbar(
            message, actionLabel, duration = when (duration) {
                SnackbarDurationM2.Short -> SnackbarDuration.Short
                SnackbarDurationM2.Long -> SnackbarDuration.Long
                SnackbarDurationM2.Indefinite -> SnackbarDuration.Indefinite
            }
        )

    /**
     * Shows or queues to be shown a [Snackbar] at the bottom of the [Scaffold] to which this state
     * is attached and suspends until the snackbar has disappeared.
     */
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) = snackBarHostState?.showSnackbar(message, actionLabel, duration = duration)
        ?: snackBarHostStateM2?.showSnackbar(
            message, actionLabel, duration = when (duration) {
                SnackbarDuration.Short -> SnackbarDurationM2.Short
                SnackbarDuration.Long -> SnackbarDurationM2.Long
                SnackbarDuration.Indefinite -> SnackbarDurationM2.Indefinite
            }
        )
}

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
suspend fun SnackbarHostStateWrapper?.showAutoDurationSnackbar(
    message: String,
    actionLabel: String? = null,
) = this?.showSnackbar(
    message = message,
    actionLabel = actionLabel,
    duration = SnackbarAttributes.determineDuration(message, actionLabel).toM3Duration()
) ?: run { Timber.d("Snackbarhost not found to show a snackbar message: $message") }
