package mega.privacy.android.app.interfaces

/**
 * Callback which allows to show a simple Snackbar (SNACKBAR_TYPE).
 */
interface SimpleSnackbarCallBack {

    /**
     * Shows a simple Snackbar (SNACKBAR_TYPE).
     *
     * @param message String to show in the Snackbar.
     */
    fun showSnackbar(message: String?)
}