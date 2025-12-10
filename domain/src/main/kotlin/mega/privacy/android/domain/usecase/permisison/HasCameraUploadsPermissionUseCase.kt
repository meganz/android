package mega.privacy.android.domain.usecase.permisison

import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

class HasCameraUploadsPermissionUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = permissionRepository.hasCameraUploadsPermission()
}
