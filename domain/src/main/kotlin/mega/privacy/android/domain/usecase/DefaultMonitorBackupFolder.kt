package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Default implementation of [MonitorBackupFolder]
 */
class DefaultMonitorBackupFolder @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val monitorUserUpdates: MonitorUserUpdates,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : MonitorBackupFolder {

    private val lazyFlow: SharedFlow<Result<NodeId>> by lazy {
        flow {
            emit(kotlin.runCatching { nodeRepository.getBackupFolderId() })
            emitAll(
                monitorUserUpdates()
                    .filter { it == UserChanges.MyBackupsFolder }
                    .map {
                        kotlin.runCatching { nodeRepository.getBackupFolderId() }
                    }.catch { emit(Result.failure(it)) }
            )
        }.shareIn(
            scope = CoroutineScope(dispatcher),
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )
    }

    override fun invoke(): SharedFlow<Result<NodeId>> {
        return lazyFlow
    }

}