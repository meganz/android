package mega.privacy.android.app.main.dialog.chatstatus

import android.app.Activity
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.UserStatus
import javax.inject.Inject

@ActivityScoped
internal class UserStatusToStringMapper @Inject constructor(
    private val activity: Activity,
) {
    operator fun invoke(status: UserStatus) = when (status) {
        UserStatus.Offline -> activity.getString(R.string.offline_status)
        UserStatus.Away -> activity.getString(R.string.away_status)
        UserStatus.Online -> activity.getString(R.string.online_status)
        UserStatus.Busy -> activity.getString(R.string.busy_status)
        UserStatus.Invalid -> ""
    }
}