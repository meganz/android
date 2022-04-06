package mega.privacy.android.app.presentation.meetings

import android.content.Context
import android.content.Intent

/**
 * The interface for OpenFileHelper
 */
interface OpenCallWrapper {

    /**
     * Get Intent to open call
     * @param context Context
     * @param chatId Chat room ID
     * @param isAudioEnabled True, audio ON. False, audio OFF.
     * @param isVideoEnabled True, video ON. False, video OFF.
     */
    fun getIntentForOpenOngoingCall(
        context: Context,
        chatId: Long,
        isAudioEnabled: Boolean,
        isVideoEnabled: Boolean,
    ): Intent?
}