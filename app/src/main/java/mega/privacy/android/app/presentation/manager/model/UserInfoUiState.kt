package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.app.presentation.avatar.model.AvatarContent

internal data class UserInfoUiState(
    val fullName: String = "",
    val email: String = "",
    val avatarContent: AvatarContent? = null,
    val isTestPasswordRequired: Boolean = false,
)