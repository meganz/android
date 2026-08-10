package mega.privacy.android.core.sharedcomponents.snackbar

import androidx.annotation.StringRes

@Deprecated(
    message = "Use SnackbarEventQueue to show snackbar",
    replaceWith = ReplaceWith("mega.privacy.android.navigation.contract.queue.SnackbarEventQueue")
)
interface SnackBarHandler {
    @Deprecated(
        message = "Use SnackbarEventQueue to show snackbar",
        replaceWith = ReplaceWith("mega.privacy.android.navigation.contract.queue.SnackbarEventQueue")
    )
    fun postSnackbarMessage(
        message: String,
        actionLabel: String? = null,
        snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
    )

    @Deprecated(
        message = "Use SnackbarEventQueue to show snackbar",
        replaceWith = ReplaceWith("mega.privacy.android.navigation.contract.queue.SnackbarEventQueue")
    )
    fun postSnackbarMessage(
        @StringRes resId: Int,
        actionLabel: String? = null,
        snackbarDuration: MegaSnackbarDuration = MegaSnackbarDuration.Short,
    )
}