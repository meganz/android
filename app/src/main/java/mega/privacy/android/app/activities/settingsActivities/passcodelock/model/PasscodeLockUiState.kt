package mega.privacy.android.app.activities.settingsActivities.passcodelock.model

/**
 * State of Passcode Lock
 * @property logoutEvent Pair of booleans, offlineFilesExist and transfersExist
 */
data class PasscodeLockUiState(
    val logoutEvent: Pair<Boolean, Boolean>? = null,
)
