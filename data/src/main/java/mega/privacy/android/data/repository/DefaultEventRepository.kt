package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.BroadcastReceiverGateway
import mega.privacy.android.domain.repository.EventRepository
import javax.inject.Inject

internal class DefaultEventRepository @Inject constructor(private val broadcastReceiverGateway: BroadcastReceiverGateway) :
    EventRepository {
    override fun monitorCameraUploadPauseState() =
        broadcastReceiverGateway.monitorCameraUploadPauseState

    override suspend fun broadcastUploadPauseState() =
        broadcastReceiverGateway.broadcastUploadPauseState()
}
