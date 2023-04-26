package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting user name by email
 */
class GetUserNameByEmailUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Get user name by email
     *
     * @param email email
     * @return user name
     */
    suspend operator fun invoke(email: String) = mediaPlayerRepository.getUserNameByEmail(email)
}