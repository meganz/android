package mega.privacy.android.core.sharedcomponents.snackbar

/**
 * Enum class to display snackbar duration
 */
@Deprecated(
    message = "Use SnackbarEventQueue to show snackbar",
    replaceWith = ReplaceWith("mega.privacy.android.navigation.contract.queue.SnackbarEventQueue")
)
enum class MegaSnackbarDuration {
    /**
     * Snackbar duration small
     */
    Short,

    /**
     * Snackbar duration long
     */
    Long,

    /**
     * Snackbar duration indefinote
     */
    Indefinite
}