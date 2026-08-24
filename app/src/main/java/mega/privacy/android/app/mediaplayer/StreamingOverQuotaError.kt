package mega.privacy.android.app.mediaplayer

import androidx.media3.common.PlaybackException
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import timber.log.Timber

/**
 * Returns whether a playback error is the streaming over-quota failure mode.
 *
 * Streaming quota errors do not arrive through the transfer event channel: the SDK's local HTTP
 * proxy answers the request headers but closes the stream with a zero-byte body, which ExoPlayer
 * surfaces as a generic IO error. So on IO errors the quota is queried directly instead of
 * waiting for an event that never comes. The whole IO error range is matched, not just
 * CONNECTION_FAILED, because the proxy's zero-byte close surfaces as different IO codes across
 * retries (unspecified, timeout, bad status).
 *
 * @param errorCode the [PlaybackException.errorCode] reported by the player.
 * @param isInTransferOverQuotaUseCase queries the current transfer quota state.
 */
internal suspend fun isStreamingOverQuotaError(
    errorCode: Int,
    isInTransferOverQuotaUseCase: IsInTransferOverQuotaUseCase,
): Boolean {
    val isIoError = errorCode in
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED..PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
    if (!isIoError) return false
    return runCatching { isInTransferOverQuotaUseCase() }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)
}
