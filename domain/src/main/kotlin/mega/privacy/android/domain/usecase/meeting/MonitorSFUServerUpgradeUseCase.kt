package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.meeting.CallNotificationType
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatCallTermCodeType
import javax.inject.Inject

/**
 * MonitorSFUServerUpgradeUseCase
 *
 * Use case for monitoring when a specific call is terminated due to SFU server upgrade.
 */
class MonitorSFUServerUpgradeUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
) {

    /**
     * Invoke
     * @return Boolean. True if the call is terminated due to SFU server upgrade.
     */
    operator fun invoke() =
        monitorChatCallUpdatesUseCase().map { call ->
            if (call.changes?.contains(ChatCallChanges.Status) == true) {
                call.status == ChatCallStatus.TerminatingUserParticipation && call.termCode == ChatCallTermCodeType.ProtocolVersion
            } else if (call.changes?.contains(ChatCallChanges.GenericNotification) == true) {
                call.notificationType == CallNotificationType.SFUError && call.termCode == ChatCallTermCodeType.ProtocolVersion
            } else {
                false
            }
        }
}
