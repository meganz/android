package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

class SetNotificationPermissionShownUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
    @SystemTime private val currentTimeProvider: () -> Long
) {
    /**
     * Invoke
     *
     * @return Boolean
     */
    suspend operator fun invoke() =
        permissionRepository.setNotificationPermissionShownTimestamp(currentTimeProvider())
}