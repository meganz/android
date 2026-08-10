package mega.privacy.android.core.passcode.presentation.model

import mega.privacy.android.domain.entity.ThemeMode

/**
 * Passcode unlock state
 */
sealed interface PasscodeUnlockState {
    val themeMode: ThemeMode

    /**
     * Loading
     */
    data class Loading(
        override val themeMode: ThemeMode,
    ) : PasscodeUnlockState

    /**
     * Data
     *
     * @property passcodeType
     * @property failedAttempts
     * @property logoutWarning
     * @property themeMode
     */
    data class Data(
        val passcodeType: PasscodeUIType,
        val failedAttempts: Int,
        val logoutWarning: Boolean,
        override val themeMode: ThemeMode,
    ) : PasscodeUnlockState
}
