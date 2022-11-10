package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.gateway.BroadcastReceiverGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

/**
 * Default implementation of [BroadcastReceiverGateway]
 */
internal class BroadcastReceiverFacade @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
) : BroadcastReceiverGateway {

    private val _monitorCameraUploadPauseState = MutableSharedFlow<Boolean>()

    override val monitorCameraUploadPauseState =
        _monitorCameraUploadPauseState.toSharedFlow(appScope)

    override suspend fun broadcastUploadPauseState() =
        _monitorCameraUploadPauseState.emit(true)
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
