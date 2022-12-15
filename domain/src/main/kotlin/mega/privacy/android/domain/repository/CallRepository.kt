package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.chat.ChatCall

/**
 * The repository interface regarding Chat calls.
 */
interface CallRepository {

    /**
     * Open call or start call and open it
     *
     * @param chatId        Chat Id
     * @param video  True for audio-video call, false for audio call
     * @param audio  True for starting a call with audio (mute disabled)
     * @return [ChatCall]
     */
    suspend fun startCall(chatId: Long, video: Boolean, audio: Boolean): ChatCall?
}