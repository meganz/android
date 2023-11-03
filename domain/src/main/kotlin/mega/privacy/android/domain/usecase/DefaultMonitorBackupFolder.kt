package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import javax.inject.Inject


/**
 * Default implementation of [MonitorBackupFolder]
 */
class DefaultMonitorBackupFolder @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val monitorLogoutUseCase: MonitorLogoutUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : MonitorBackupFolder {

    private val flow: SharedFlow<Result<NodeId>> =
        channelFlow<Result<NodeId>> {
            monitorUserUpdates()
                .filter { userUpdate -> userUpdate == UserChanges.MyBackupsFolder }
                .onEach { trySend(getBackupFolderId()) }
                .launchIn(applicationScope)

            monitorFetchNodesFinishUseCase()
                .onEach { trySend(getBackupFolderId()) }
                .launchIn(applicationScope)

            monitorLogoutUseCase()
                .onEach { trySend(Result.failure(NodeDoesNotExistsException())) }
                .launchIn(applicationScope)

            awaitClose()
        }.onStart {
            emit(getBackupFolderId())
        }.shareIn(
            scope = applicationScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    override fun invoke(): SharedFlow<Result<NodeId>> = flow

    private suspend fun getBackupFolderId() = kotlin.runCatching {
        nodeRepository.getBackupFolderId()
    }

}
