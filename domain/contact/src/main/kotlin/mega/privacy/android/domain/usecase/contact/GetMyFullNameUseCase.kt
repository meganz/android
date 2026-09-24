package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get my full name use case
 *
 * @property repository
 */
class GetMyFullNameUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @return String
     */
    suspend operator fun invoke() = repository.getMyFullName()
}