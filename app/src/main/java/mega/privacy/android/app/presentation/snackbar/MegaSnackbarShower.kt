package mega.privacy.android.app.presentation.snackbar

import androidx.compose.material.SnackbarDuration

/**
 * Interface to show snackbar
 */
interface MegaSnackbarShower {

    /**
     * show snackbar
     *
     * @param message text to be shown in the Snackbar
     * @param actionLabel optional action label to show as button in the Snackbar
     * @param duration duration to control how long snackbar will be shown in [SnackbarHost], either
     * [MegaSnackbarDuration.Short], [MegaSnackbarDuration.Long] or [MegaSnackbarDuration.Indefinite]
     */
    fun showMegaSnackbar(
        message: String,
        actionLabel: String?,
        duration: MegaSnackbarDuration,
    )
}