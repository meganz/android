package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Use case to check if the meeting permissions education dialog has already been shown.
 */
class IsMeetingPermissionsEducationShownUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
) {
    /**
     * Invoke
     *
     * @return true if the dialog has already been shown, false otherwise
     */
    suspend operator fun invoke() = permissionRepository.isMeetingPermissionsEducationShown()
}
