package mega.privacy.android.app.presentation.settings.compose.home.mapper

import mega.privacy.android.app.presentation.settings.compose.home.model.MyAccountSettingsState
import mega.privacy.android.domain.entity.UserAccount
import javax.inject.Inject

internal class MyAccountSettingStateMapper @Inject constructor() {
    operator fun invoke(userAccount: UserAccount) = with(userAccount) {
        val id = userId ?: return@with null
        MyAccountSettingsState(
            userId = id,
            name = fullName ?: email,
            email = email
        )
    }
}