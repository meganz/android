package mega.privacy.android.domain.usecase.avatar

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Set avatar use case
 *
 * @property repository
 */
class SetAvatarUseCase @Inject constructor(
    private val repository: AvatarRepository,
) {
    /**
     * Invoke
     *
     * @param filePath
     */
    suspend operator fun invoke(filePath: String?) {
        repository.setAvatar(filePath)
    }
}