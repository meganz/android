package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Get call handle list use case
 *
 * @property repository
 */
class GetCallHandleListUseCase @Inject constructor(
    private val repository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param status
     */
    suspend operator fun invoke(status: ChatCallStatus) = repository.getCallHandleList(status)
}