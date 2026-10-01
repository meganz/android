package mega.privacy.android.domain.usecase.avatar

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Use case for getting the avatar of a user.
 */
class GetUserAvatarUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     * @return The user's avatar file if any.
     */
    suspend operator fun invoke(userHandle: Long) = avatarRepository.getAvatarFile(userHandle)
}