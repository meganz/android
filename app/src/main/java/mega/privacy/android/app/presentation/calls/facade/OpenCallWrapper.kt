package mega.privacy.android.app.presentation.calls.facade

import android.content.Context
import android.content.Intent

/**
 * The interface for OpenFileHelper
 */
interface OpenCallWrapper {

    /**
     * Get Intent to open file
     * @param context Context
     * @param actionForCall Action
     */
    fun getIntentForOpenCreateMeeting(
        context: Context,
        actionForCall: String
    ): Intent?

    /**
     * Get Intent to open file
     * @param context Context
     * @param actionForCall Action
     * @param chatId Chat room ID
     */
    fun getIntentForOpenRingingCall(
        context: Context,
        actionForCall: String,
        chatId: Long
    ): Intent?

    /**
     * Get Intent to open file
     * @param context Context
     * @param actionForCall Action
     * @param chatId Chat room ID
     * @param isAudioEnabled True, audio ON. False, audio OFF.
     * @param isVideoEnabled True, video ON. False, video OFF.
     */
    fun getIntentForOpenOngoingCall(
        context: Context,
        actionForCall: String,
        chatId: Long,
        isAudioEnabled: Boolean?,
        isVideoEnabled: Boolean?
    ): Intent?

    /**
     * Get Intent to open file
     * @param context Context
     * @param actionForCall Action
     * @param chatId Chat room ID
     * @param meetingName Meeting name
     * @param meetingLink Meeting link
     * @param publicChatHandle chat handle
     */
    fun getIntentForOpenJoinMeeting(
        context: Context,
        actionForCall: String,
        chatId: Long,
        meetingName: String,
        meetingLink: String,
        publicChatHandle: Long?
    ): Intent?
}