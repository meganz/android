package mega.privacy.android.domain.usecase.avatar

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Use case for getting the user's avatar secondary color.
 */
class GetUserAvatarSecondaryColorUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     * @return The user's avatar secondary color.
     */
    suspend operator fun invoke(userHandle: Long) =
        avatarRepository.getAvatarSecondaryColor(userHandle)
}
