package mega.privacy.android.app.presentation.permissions

import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.domain.entity.ThemeMode

data class PermissionsUIState(
    val isOnboardingRevampEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val visiblePermission: PermissionScreen? = null,
)