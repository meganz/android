package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.ManagerStateRepository
import javax.inject.Inject

/**
 * Default set parent handle in manager section
 *
 * @property managerStateRepository
 */
class DefaultSetManagerParentHandle @Inject constructor(
    private val managerStateRepository: ManagerStateRepository
) : SetManagerParentHandle {
    override fun invoke(managerParentHandleType: SetManagerParentHandleType, parentHandle: Long) =
        when(managerParentHandleType) {
            SetManagerParentHandleType.RubbishBin -> managerStateRepository.setRubbishBinParentHandle(parentHandle)
            SetManagerParentHandleType.Browser -> managerStateRepository.setBrowserParentHandle(parentHandle)
        }
}