package mega.privacy.android.domain.usecase.permisison

import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Has notification permission use case
 *
 */
class HasNotificationPermissionUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke(): Boolean = permissionRepository.hasNotificationPermission()
}
