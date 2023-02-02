package mega.privacy.android.app.exportRK.model

/**
 * UI State for ExportKeyRecoveryActivity.kt
 */
sealed interface RecoveryKeyUIState {
    /**
     * UI State when user copies the recovery key
     * @param key the recovery key
     */
    data class CopyRecoveryKey(val key: String?) : RecoveryKeyUIState

    /**
     * UI State when user exports the recovery key
     * @param key the recovery key
     */
    data class ExportRecoveryKey(val key: String?) : RecoveryKeyUIState

    /**
     * UI State when user prints the recovery key
     */
    object PrintRecoveryKey : RecoveryKeyUIState
}