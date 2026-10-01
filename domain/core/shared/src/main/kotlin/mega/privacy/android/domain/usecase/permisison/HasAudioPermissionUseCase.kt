package mega.privacy.android.domain.usecase.permisison

import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Has Audio permission use case
 *
 */
class HasAudioPermissionUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = permissionRepository.hasAudioPermission()
}