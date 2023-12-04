package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for enabling geolocation.
 */
class EnableGeolocationUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @return True if geolocation is enabled, false otherwise.
     */
    suspend operator fun invoke() = chatRepository.enableGeolocation()
}