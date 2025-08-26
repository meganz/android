package mega.privacy.android.core.sharedcomponents.snackbar

import androidx.annotation.StringRes

@Deprecated(
    message = "Use SnackbarEventQueue to show snackbar",
    replaceWith = ReplaceWith("mega.privacy.android.navigation.contract.queue.SnackbarEventQueue")
)
interface SnackBarHandler {
    fun postSnackbarMessage(
        message: String,
        actionLabel: String? = null,
        snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
    )

    fun postSnackbarMessage(
        @StringRes resId: Int,
        actionLabel: String? = null,
        snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
    )
}