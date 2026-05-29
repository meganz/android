package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.MutePushNotificationDialog
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/** What the dialog should mute. */
sealed interface MuteTarget {
    data object Global : MuteTarget
    data class Single(val chatId: Long, val isMeeting: Boolean) : MuteTarget
    data class Multiple(val chatIds: List<Long>, val isMeeting: Boolean) : MuteTarget
}

/**
 * Compose dialog for muting chat notifications. [onMuteResult] is invoked with the applied
 * option once the repository call succeeds; [onDismissRequest] fires after it (and on cancel).
 */
@Composable
internal fun MuteChatDialog(
    target: MuteTarget,
    onDismissRequest: () -> Unit,
    onMuteResult: (ChatPushNotificationMuteOption) -> Unit,
    viewModel: MuteDialogViewModel = hiltViewModel(),
) {
    val options = remember(target, viewModel) { viewModel.muteOptionsFor(target) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.muteResultEvent,
        onConsumed = viewModel::onMuteResultEventConsumed,
    ) { option ->
        onMuteResult(option)
        onDismissRequest()
    }

    MuteChatDialogContent(
        target = target,
        options = options,
        onCancel = onDismissRequest,
        onConfirm = { option -> viewModel.applyMute(target, option) },
    )
}

@Composable
internal fun MuteChatDialogContent(
    target: MuteTarget,
    options: List<ChatPushNotificationMuteOption>,
    onCancel: () -> Unit,
    onConfirm: (ChatPushNotificationMuteOption) -> Unit,
) {
    val isMeeting = when (target) {
        MuteTarget.Global -> false
        is MuteTarget.Single -> target.isMeeting
        is MuteTarget.Multiple -> target.isMeeting
    }
    MutePushNotificationDialog(
        state = options,
        isMeeting = isMeeting,
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}

private val GlobalPreviewOptions = listOf(
    ChatPushNotificationMuteOption.Mute30Minutes,
    ChatPushNotificationMuteOption.Mute1Hour,
    ChatPushNotificationMuteOption.Mute6Hours,
    ChatPushNotificationMuteOption.Mute24Hours,
    ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
)

private val ChatPreviewOptions = listOf(
    ChatPushNotificationMuteOption.Mute30Minutes,
    ChatPushNotificationMuteOption.Mute1Hour,
    ChatPushNotificationMuteOption.Mute6Hours,
    ChatPushNotificationMuteOption.Mute24Hours,
    ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
)

@CombinedThemeComponentPreviews
@Composable
private fun MuteChatDialogGlobalPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MuteChatDialogContent(
            target = MuteTarget.Global,
            options = GlobalPreviewOptions,
            onCancel = {},
            onConfirm = {},
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun MuteChatDialogSingleChatPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MuteChatDialogContent(
            target = MuteTarget.Single(chatId = 1L, isMeeting = false),
            options = ChatPreviewOptions,
            onCancel = {},
            onConfirm = {},
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun MuteChatDialogSingleMeetingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MuteChatDialogContent(
            target = MuteTarget.Single(chatId = 1L, isMeeting = true),
            options = ChatPreviewOptions,
            onCancel = {},
            onConfirm = {},
        )
    }
}
