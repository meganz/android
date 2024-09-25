package mega.privacy.android.domain.usecase.user

import mega.privacy.android.domain.repository.AvatarRepository
import javax.inject.Inject

/**
 * Default implementation of [MonitorUserAvatarUpdatesUseCase]
 *
 * @property avatarRepository
 * @constructor Create empty Default monitor user updates
 */

class MonitorUserAvatarUpdatesUseCase @Inject constructor(
    private val avatarRepository: AvatarRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = avatarRepository.monitorUserAvatarUpdates()
}