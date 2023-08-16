package mega.privacy.android.domain.usecase.avatar

import mega.privacy.android.domain.repository.AvatarRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case interface to get my avatar file
 */
class GetMyAvatarFileUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {
    /**
     * invoke
     */
    suspend operator fun invoke(isForceRefresh: Boolean): File? =
        avatarRepository.getMyAvatarFile(isForceRefresh)
}