package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get My User Handle Use Case
 *
 */
class GetMyUserHandleUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.getMyUserHandle()
}