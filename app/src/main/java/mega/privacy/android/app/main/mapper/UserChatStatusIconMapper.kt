package mega.privacy.android.app.main.mapper

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import javax.inject.Inject

/**
 * User chat status icon mapper
 *
 */
class UserChatStatusIconMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param status User chat status
     * @param isDarkTheme Is dark theme
     * @return
     */
    operator fun invoke(status: UserChatStatus, isDarkTheme: Boolean): Int = when (status) {
        UserChatStatus.Offline -> if (isDarkTheme) R.drawable.ic_offline_dark_drawer else R.drawable.ic_offline_light
        UserChatStatus.Away -> if (isDarkTheme) R.drawable.ic_away_dark_drawer else R.drawable.ic_away_light
        UserChatStatus.Online -> if (isDarkTheme) R.drawable.ic_online_dark_drawer else R.drawable.ic_online_light
        UserChatStatus.Busy -> if (isDarkTheme) R.drawable.ic_busy_dark_drawer else R.drawable.ic_busy_light
        UserChatStatus.Invalid -> 0
    }
}