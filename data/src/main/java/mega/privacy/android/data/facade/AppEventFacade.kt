package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

/**
 * Default implementation of [AppEventGateway]
 */
internal class AppEventFacade @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
) : AppEventGateway {

    private val _monitorCameraUploadPauseState = MutableSharedFlow<Boolean>()
    private val _transferOverQuota = MutableSharedFlow<Boolean>()
    private val logout = MutableSharedFlow<Boolean>()
    private val _transferFailed = MutableSharedFlow<Boolean>()

    private val _isSMSVerificationShownState = MutableStateFlow(false)

    override val monitorCameraUploadPauseState =
        _monitorCameraUploadPauseState.toSharedFlow(appScope)

    override suspend fun broadcastUploadPauseState() =
        _monitorCameraUploadPauseState.emit(true)

    override suspend fun setSMSVerificationShown(isShown: Boolean) {
        _isSMSVerificationShownState.value = isShown
    }

    override suspend fun isSMSVerificationShown(): Boolean = _isSMSVerificationShownState.value

    override fun monitorTransferOverQuota(): Flow<Boolean> = _transferOverQuota.asSharedFlow()

    override suspend fun broadcastTransferOverQuota() {
        _transferOverQuota.emit(true)
    }

    override fun monitorLogout(): Flow<Boolean> = logout.asSharedFlow()

    override suspend fun broadcastLogout() = logout.emit(true)

    override fun monitorFailedTransfer(): Flow<Boolean> = _transferFailed.asSharedFlow()

    override suspend fun broadcastFailedTransfer(isFailed: Boolean) {
        _transferFailed.emit(isFailed)
    }
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
