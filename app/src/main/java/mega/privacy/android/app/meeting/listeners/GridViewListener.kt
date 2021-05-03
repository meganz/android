package mega.privacy.android.app.meeting.listeners

import mega.privacy.android.app.meeting.adapter.Participant
import nz.mega.sdk.MegaChatVideoListenerInterface

interface GridViewListener {
    /**
     * Listener for button bar
     */
    fun onCloseVideo(participant: Participant)
    fun onActivateVideo(participant: Participant)
}