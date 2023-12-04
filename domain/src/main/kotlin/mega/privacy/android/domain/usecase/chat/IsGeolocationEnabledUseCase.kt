package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for checking if geolocation is enabled.
 */
class IsGeolocationEnabledUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @return True if geolocation is enabled, false otherwise.
     */
    suspend operator fun invoke() = chatRepository.isGeolocationEnabled()
}