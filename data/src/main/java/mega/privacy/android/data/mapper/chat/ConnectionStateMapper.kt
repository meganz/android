package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ConnectionState
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject


/**
 * ConnectionStateMapper
 *
 * Maps MegaChatAPI Connection to [ConnectionState]
 */
internal class ConnectionStateMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param state MegaChatAPI Connection state
     */
    operator fun invoke(state: Int): ConnectionState = when (state) {
        MegaChatApi.CONNECTED -> ConnectionState.Connected
        MegaChatApi.CONNECTING -> ConnectionState.Connecting
        else -> ConnectionState.Disconnected
    }
}