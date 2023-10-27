package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class OfflineAvailabilityMapper @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileGateway: FileGateway,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
) {
    suspend operator fun invoke(
        megaNode: MegaNode,
        offline: Offline,
    ): Boolean = withContext(ioDispatcher) {
        if (offline.lastModifiedTime <= 0) {
            fileGateway.getLocalFile(
                fileName = megaNode.name,
                fileSize = megaNode.size,
                lastModifiedDate = megaNode.modificationTime
            )?.let {
                val lastModifiedFileTime = it.lastModified()
                    .milliseconds
                    .inWholeSeconds
                val offlineWithTimeStamp = offline.copy(
                    lastModifiedTime = lastModifiedFileTime
                )
                megaLocalRoomGateway.saveOfflineInformation(offlineWithTimeStamp)
                lastModifiedFileTime >= megaNode.modificationTime
            } ?: false
        } else {
            offline.lastModifiedTime >= megaNode.modificationTime
        }
    }
}