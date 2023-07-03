package mega.privacy.android.domain.usecase.avatar

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Use case for getting the user's avatar color.
 */
class GetUserAvatarColorUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     * @return The user's avatar color.
     */
    suspend operator fun invoke(userHandle: Long) = avatarRepository.getAvatarColor(userHandle)
}