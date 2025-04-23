package mega.privacy.android.app.presentation.permissions

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.ThemeMode

data class PermissionsUIState(
    val isOnboardingRevampEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val visiblePermission: NewPermissionScreen = NewPermissionScreen.Loading,
    val finishEvent: StateEvent = consumed
)

enum class NewPermissionScreen {
    Notification,
    CameraBackup,
    Loading
}