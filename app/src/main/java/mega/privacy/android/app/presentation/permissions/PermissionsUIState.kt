package mega.privacy.android.app.presentation.permissions

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.ThemeMode

data class PermissionsUIState(
    val themeMode: ThemeMode = ThemeMode.System,
    val visiblePermission: NewPermissionScreen = NewPermissionScreen.Loading,
    val finishEvent: StateEvent = consumed,
    val isCameraUploadsEnabled: Boolean = false,
)

enum class NewPermissionScreen {
    Notification,
    CameraBackup,
    Loading
}