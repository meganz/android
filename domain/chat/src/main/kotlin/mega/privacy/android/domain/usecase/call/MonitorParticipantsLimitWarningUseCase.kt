package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Use case that tells whether the call user-limit warning should be shown for chat [chatId]'s
 * active call. The warning appears when the "unlimited pro plan" feature is enabled and the active
 * call has reached its user limit. Backs the "add meeting participants" picker banner.
 *
 * @property getFeatureFlagValueUseCase
 * @property getChatCallUseCase
 * @property monitorChatCallUpdatesUseCase
 */
class MonitorParticipantsLimitWarningUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId the chat whose active call is monitored.
     * @return a [Flow] emitting true while the active call has reached its user limit.
     */
    operator fun invoke(chatId: Long): Flow<Boolean> = flow {
        if (!getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)) {
            emit(false)
            return@flow
        }
        emitAll(
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == chatId }
                .map { it.isUserLimitReached() }
                .onStart { emit(getChatCallUseCase(chatId).isUserLimitReached()) }
                .distinctUntilChanged()
        )
    }

    private fun ChatCall?.isUserLimitReached(): Boolean {
        val limit = this?.callUsersLimit ?: return false
        val participants = numParticipants ?: return false
        return participants >= limit && status != ChatCallStatus.Destroyed
    }
}
