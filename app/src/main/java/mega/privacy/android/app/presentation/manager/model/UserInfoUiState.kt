package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.shared.account.presentation.model.AvatarContent

internal data class UserInfoUiState(
    val fullName: String = "",
    val email: String = "",
    val avatarContent: AvatarContent? = null,
    val isTestPasswordRequired: Boolean = false,
)