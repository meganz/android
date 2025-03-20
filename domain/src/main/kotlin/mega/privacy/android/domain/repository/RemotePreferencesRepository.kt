package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.chat.MeetingTooltipItem

/**
 * Repository for Application Preferences persisted in a user attribute
 */
interface RemotePreferencesRepository {

    /**
     * Set scheduled meetings tooltip preference
     *
     * @param item   [MeetingTooltipItem]
     */
    suspend fun setMeetingTooltipPreference(item: MeetingTooltipItem)

    /**
     * Get scheduled meetings tooltip preference
     *
     * @return  [MeetingTooltipItem]
     */
    suspend fun getMeetingTooltipPreference(): MeetingTooltipItem

    /**
     * Set note to self chat new label preference
     *
     * @param counter
     */
    suspend fun setNoteToSelfChatNewLabelPreference(counter: String)

    /**
     * Get note to self chat new label preference
     *
     * @return  counter
     */
    suspend fun getNoteToSelfChatNewLabelPreference(): String
}
