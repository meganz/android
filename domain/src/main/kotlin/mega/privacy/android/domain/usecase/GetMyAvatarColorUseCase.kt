package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Get avatar color
 */
class GetMyAvatarColorUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {
    /**
     * get color from the avatar
     *
     * @return the color of avatar
     */
    suspend operator fun invoke(): Int = avatarRepository.getMyAvatarColor()
}