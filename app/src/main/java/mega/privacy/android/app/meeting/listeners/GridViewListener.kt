package mega.privacy.android.app.meeting.listeners

import mega.privacy.android.app.meeting.adapter.Participant
import nz.mega.sdk.MegaChatSession

interface GridViewListener {
    fun onCloseVideo(session: MegaChatSession?, participant: Participant)

    fun onActivateVideo(session: MegaChatSession?, participant: Participant)
}