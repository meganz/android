package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.backup.BackupMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * implementation of [BackupRepository]
 * @property backupMapper [BackupMapper]
 * @property ioDispatcher [CoroutineDispatcher]

 */
internal class BackupRepositoryImpl @Inject constructor(
    private val backupMapper: BackupMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : BackupRepository {
    override suspend fun getDeviceName(deviceId: String): String? = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getDeviceName") {
                it.name
            }
            megaApiGateway.getDeviceName(
                deviceId = deviceId,
                listener = listener,
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun setBackup(
        backupType: Int,
        targetNode: Long,
        localFolder: String,
        backupName: String,
        state: Int,
        subState: Int,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setBackup") {
                backupMapper(it)
            }
            megaApiGateway.setBackup(
                backupType = backupType,
                targetNode = targetNode,
                localFolder = localFolder,
                backupName = backupName,
                state = state,
                subState = subState,
                listener = listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }
}
