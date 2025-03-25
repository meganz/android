package mega.privacy.android.app.presentation.settings.compose.home.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.domain.entity.user.UserId

@Immutable
data class MyAccountSettingsState(
    val userId: UserId,
    val name: String,
    val email: String,
)
