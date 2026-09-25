package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.call.CallNotificationType
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
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