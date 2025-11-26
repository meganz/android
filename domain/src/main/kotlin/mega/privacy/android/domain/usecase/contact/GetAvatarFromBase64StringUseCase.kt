package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Contact link query use case
 *
 * @property repository
 */
class GetAvatarFromBase64StringUseCase @Inject constructor(
    private val repository: AvatarRepository,
) {
    /**
     * Invoke
     *
     * @param userHandle
     */
    suspend operator fun invoke(userHandle: Long, base64String: String) =
        repository.getAvatarFromBase64String(userHandle, base64String)
}