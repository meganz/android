package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import javax.inject.Inject

/**
 * A use case to determines whether there are ongoing video calls.
 *
 * @property getChatCallInProgress Use case for getting the in progress call.
 * @property defaultDispatcher A [CoroutineDispatcher] to execute the process.
 */
class AreThereOngoingVideoCallsUseCase @Inject constructor(
    private val getChatCallInProgress: GetChatCallInProgress,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invocation method.
     *
     * @return Boolean. Whether there are ongoing video calls.
     */
    suspend operator fun invoke(): Boolean = withContext(defaultDispatcher) {
        getChatCallInProgress()?.hasLocalVideo == true
    }
}
