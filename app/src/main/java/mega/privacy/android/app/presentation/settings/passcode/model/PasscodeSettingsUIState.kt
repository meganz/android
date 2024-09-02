package mega.privacy.android.app.presentation.settings.passcode.model

/**
 * Passcode settings u i state
 *
 * @property isEnabled
 * @property isBiometricsEnabled
 * @property timeout
 */
data class PasscodeSettingsUIState(
    val isEnabled: Boolean,
    val isBiometricsEnabled: Boolean,
    val timeout: TimeoutOption?,
) {
    companion object {
        /**
         * Initial state
         */
        val INITIAL = PasscodeSettingsUIState(
            isEnabled = false,
            isBiometricsEnabled = false,
            timeout = null,
        )
    }
}