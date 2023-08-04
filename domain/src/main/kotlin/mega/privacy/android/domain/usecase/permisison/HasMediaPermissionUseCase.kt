package mega.privacy.android.domain.usecase.permisison

import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Has media permission use case
 *
 */
class HasMediaPermissionUseCase @Inject constructor(
    private val repository: PermissionRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = repository.hasMediaPermission()
}