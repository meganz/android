package mega.privacy.android.app.presentation.videoplayer.mapper

import androidx.media3.common.PlaybackException
import mega.privacy.android.app.presentation.videoplayer.model.PlayerErrorType
import javax.inject.Inject

/**
 * Mapper to convert a playback error code and network state to a [PlayerErrorType].
 */
class PlayerErrorTypeMapper @Inject constructor() {

    /**
     * Maps [errorCode] and [isConnected] to the appropriate [PlayerErrorType].
     *
     * @param errorCode The error code from [PlaybackException].
     * @param isConnected Whether the device currently has a network connection.
     * @return The corresponding [PlayerErrorType].
     */
    operator fun invoke(errorCode: Int, isConnected: Boolean): PlayerErrorType = when {
        errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
            PlayerErrorType.FILE_NOT_SUPPORTED

        !isConnected -> PlayerErrorType.NO_NETWORK

        else -> PlayerErrorType.CANNOT_PLAY
    }
}
