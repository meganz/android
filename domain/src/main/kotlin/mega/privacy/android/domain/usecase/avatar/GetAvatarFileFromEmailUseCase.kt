package mega.privacy.android.domain.usecase.avatar

import mega.privacy.android.domain.repository.AvatarRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case interface to get my avatar file
 */
class GetAvatarFileFromEmailUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke(userEmail: String, skipCache: Boolean): File =
        avatarRepository.getAvatarFile(userEmail, skipCache)
}