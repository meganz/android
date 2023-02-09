package mega.privacy.android.app.presentation.settings.exportrecoverykey.model

/**
 * Recovery Key UI State
 * @param isActionGroupVertical - The orientation for the Button group
 * @param snackBarMessage - The SnackBar message, null will hide the SnackBar
 */
data class RecoveryKeyUIState(
    val isActionGroupVertical: Boolean = false,
    val snackBarMessage: String? = null
)