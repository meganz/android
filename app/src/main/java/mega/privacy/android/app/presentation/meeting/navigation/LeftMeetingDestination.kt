import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_FREE_PLAN_USERS_LIMIT
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_PARTICIPANTS_LIMIT
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LeftMeetingNavKey

fun EntryProviderScope<NavKey>.leftMeetingDestination(removeDestination: (NavKey) -> Unit) {
    entry<LeftMeetingNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, LeftMeetingActivity::class.java).apply {
                putExtra(MEETING_FREE_PLAN_USERS_LIMIT, key.callEndedDueToFreePlanLimits)
                putExtra(MEETING_PARTICIPANTS_LIMIT, key.callEndedDueToTooManyParticipants)
            }
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination(key)
        }
    }
}
