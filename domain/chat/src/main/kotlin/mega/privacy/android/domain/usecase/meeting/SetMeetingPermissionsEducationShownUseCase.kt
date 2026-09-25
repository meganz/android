package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

/**
 * Use case to mark the meeting permissions education dialog as shown.
 */
class SetMeetingPermissionsEducationShownUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = permissionRepository.setMeetingPermissionsEducationShown()
}
