package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject


/**
 * Default implementation of [MonitorBackupFolder]
 */
class DefaultMonitorBackupFolder @Inject constructor(
    private val fileRepository: FileRepository,
    private val monitorUserUpdates: MonitorUserUpdates,
) : MonitorBackupFolder {
    override fun invoke(): Flow<NodeId> {
        return flow {
            emit(fileRepository.getBackupFolderId())
            emitAll(
                monitorUserUpdates()
                    .filter { it == UserChanges.MyBackupsFolder }
                    .map {
                        fileRepository.getBackupFolderId()
                    }
            )
        }
    }
}