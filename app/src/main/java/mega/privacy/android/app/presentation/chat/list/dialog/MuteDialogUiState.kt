package mega.privacy.android.app.presentation.chat.list.dialog

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption

/**
 * UI state for [MuteChatDialog]. [muteResultEvent] fires with the applied option once the
 * repository call completes.
 */
internal data class MuteDialogUiState(
    val muteResultEvent: StateEventWithContent<ChatPushNotificationMuteOption> = consumed(),
)
