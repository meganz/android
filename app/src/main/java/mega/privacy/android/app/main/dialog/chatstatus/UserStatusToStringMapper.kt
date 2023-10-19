package mega.privacy.android.app.main.dialog.chatstatus

import android.app.Activity
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import javax.inject.Inject

@ActivityScoped
internal class UserStatusToStringMapper @Inject constructor(
    private val activity: Activity,
) {
    operator fun invoke(status: UserChatStatus) = when (status) {
        UserChatStatus.Offline -> activity.getString(R.string.offline_status)
        UserChatStatus.Away -> activity.getString(R.string.away_status)
        UserChatStatus.Online -> activity.getString(R.string.online_status)
        UserChatStatus.Busy -> activity.getString(R.string.busy_status)
        UserChatStatus.Invalid -> ""
    }
}