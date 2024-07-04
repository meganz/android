package mega.privacy.android.app.meeting.fragments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.shared.original.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import java.util.concurrent.TimeUnit

/**
 * Composable to show a banner with a message.
 */
@Composable
private fun Banner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        MiddleEllipsisText(
            text = message,
            color = TextColor.Primary,
            style = MaterialTheme.typography.body2
        )
    }
}

private const val BANNER_ANIMATION_DURATION = 1500L

/**
 * Composable to show a banner with a message.
 */
@Composable
fun MeetingBanner(inMeetingViewModel: InMeetingViewModel) {
    val context = LocalContext.current
    val state by inMeetingViewModel.state.collectAsStateWithLifecycle()
    val showWaitingForOthersBanner by inMeetingViewModel.showWaitingForOthersBanner.collectAsStateWithLifecycle()
    var shouldShowFreeCallWarning by rememberSaveable {
        mutableStateOf(true)
    }
    var text by rememberSaveable {
        mutableStateOf("")
    }

    val shouldShowParticipantsChanges = remember {
        MutableTransitionState(false)
    }

    LaunchedEffect(state.participantsChanges) {
        if (state.participantsChanges != null) {
            shouldShowParticipantsChanges.targetState = true
            delay(BANNER_ANIMATION_DURATION)
            shouldShowParticipantsChanges.targetState = false
            inMeetingViewModel.checkShowOnlyMeBanner()
            inMeetingViewModel.onConsumeParticipantChanges()
        } else {
            shouldShowParticipantsChanges.targetState = false
            inMeetingViewModel.onConsumeParticipantChanges()
        }
    }

    LaunchedEffect(state.showOnlyMeEndCallTime) {
        state.showOnlyMeEndCallTime?.let {
            var time = TimeUnit.MILLISECONDS.toSeconds(it)
            shouldShowFreeCallWarning = true
            while (it > 0) {
                text = context.getString(
                    R.string.calls_call_screen_count_down_timer_to_end_call,
                    TimeUtils.getMinutesAndSecondsFromMilliseconds(
                        TimeUnit.SECONDS.toMillis(
                            time
                        )
                    )
                )
                time -= 1
                delay(1000)
            }
            shouldShowFreeCallWarning = false
        } ?: run {
            shouldShowFreeCallWarning = false
        }
    }

    LaunchedEffect(
        key1 = state.minutesToEndMeeting,
        key2 = state.showMeetingEndWarningDialog,
        key3 = state.participantsChanges
    ) {
        if (state.minutesToEndMeeting != null && !state.showMeetingEndWarningDialog) {
            text = context.getString(
                R.string.meetings_in_call_warning_timer_message,
                TimeUtils.getMinutesAndSecondsFromMilliseconds(
                    TimeUnit.MINUTES.toMillis(
                        state.minutesToEndMeeting?.toLong() ?: 0
                    )
                )
            )
            shouldShowFreeCallWarning = true
        }
        if (state.showMeetingEndWarningDialog) {
            shouldShowFreeCallWarning = false
        }
    }
    if (shouldShowFreeCallWarning && shouldShowParticipantsChanges.targetState.not() && text.isNotBlank()) {
        Banner(text)
    }
    LaunchedEffect(key1 = showWaitingForOthersBanner) {
        if (showWaitingForOthersBanner) {
            shouldShowFreeCallWarning = false
        }
    }
    if (showWaitingForOthersBanner && shouldShowFreeCallWarning.not() && shouldShowParticipantsChanges.targetState.not()) {
        Banner(stringResource(id = R.string.calls_call_screen_waiting_for_participants))
    }

    AnimatedVisibility(visibleState = shouldShowParticipantsChanges) {
        state.participantsChanges?.let {
            Banner(it.text)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CallBannerComposePreview() {
    OriginalTempTheme(isDark = false) {
        Banner("This is a banner message")
    }
}
