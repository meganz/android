package mega.privacy.android.app.presentation.manager.model

import mega.privacy.android.feature.myaccount.presentation.model.AvatarContent

internal data class UserInfoUiState(
    val fullName: String = "",
    val email: String = "",
    val avatarContent: AvatarContent? = null,
    val isTestPasswordRequired: Boolean = false,
)