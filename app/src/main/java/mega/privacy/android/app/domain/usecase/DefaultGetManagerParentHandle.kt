package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.ManagerStateRepository
import javax.inject.Inject

/**
 * Default get parent handle in manager section
 *
 * @property managerStateRepository
 */
class DefaultGetManagerParentHandle @Inject constructor(
    private val managerStateRepository: ManagerStateRepository
) : GetManagerParentHandle {
    override fun invoke(managerParentHandleType: GetManagerParentHandleType): Long =
        when(managerParentHandleType) {
            GetManagerParentHandleType.RubbishBin -> managerStateRepository.getRubbishBinParentHandle()
            GetManagerParentHandleType.Browser -> managerStateRepository.getBrowserParentHandle()
        }
}