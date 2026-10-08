package mega.privacy.android.app.presentation.meeting.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_FREE_PLAN_USERS_LIMIT
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_PARTICIPANTS_LIMIT
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.chat.navigation.LeftMeetingEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.CreateAccountNavKey
import mega.privacy.android.navigation.destination.LeftMeetingNavKey

/**
 * Navigation destination for the screen a guest lands on after leaving a meeting. Behind
 * [ApiFeatures.CustomisableBottomNavigation] either renders the Compose [LeftMeetingEntry] inline
 * (flag on) or launches the legacy [LeftMeetingActivity] and pops the entry (flag off).
 *
 * The flag is a remote read and this screen is reached after the guest has logged out, but
 * [ApiFeatures.CustomisableBottomNavigation] is checked once per run and is already read at app
 * start to build the bottom navigation, so the value taken while the guest was signed in is the
 * one that answers here.
 */
fun EntryProviderScope<NavKey>.leftMeetingDestination(navigationHandler: NavigationHandler) {
    entry<LeftMeetingNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.CustomisableBottomNavigation,
            disabled = {
                LegacyLeftMeetingEntry(key) { navigationHandler.remove(key) }
            },
            enabled = {
                LeftMeetingEntry(
                    callEndedDueToFreePlanLimits = key.callEndedDueToFreePlanLimits,
                    callEndedDueToTooManyParticipants = key.callEndedDueToTooManyParticipants,
                    onClose = navigationHandler::back,
                    onCreateAccount = {
                        navigationHandler.navigate(CreateAccountNavKey())
                        navigationHandler.remove(key)
                    },
                )
            },
        )
    }
}

/**
 * Launches the legacy [LeftMeetingActivity] and pops the entry.
 */
@Composable
private fun LegacyLeftMeetingEntry(
    key: LeftMeetingNavKey,
    removeDestination: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val intent = Intent(context, LeftMeetingActivity::class.java).apply {
            putExtra(MEETING_FREE_PLAN_USERS_LIMIT, key.callEndedDueToFreePlanLimits)
            putExtra(MEETING_PARTICIPANTS_LIMIT, key.callEndedDueToTooManyParticipants)
        }
        context.startActivity(intent)
        removeDestination()
    }
}
