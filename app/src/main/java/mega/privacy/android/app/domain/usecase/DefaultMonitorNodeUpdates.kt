package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default monitor node updates implementation
 *
 * @property fileRepository
 */
class DefaultMonitorNodeUpdates @Inject constructor(
    private val fileRepository: FilesRepository
) : MonitorNodeUpdates {
    override fun invoke(): Flow<List<MegaNode>> = fileRepository.monitorNodeUpdates()
}