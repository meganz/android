package mega.privacy.android.domain.entity.chat

/**
 * Scheduled meeting changes.
 */
enum class ScheduledMeetingChanges {
    /**
     *  New scheduled meeting
     */
    NewScheduledMeeting,

    /**
     *  Parent scheduled meeting id has changed
     */
    ParentScheduledMeetingId,

    /**
     * Timezone has changed
     */
    TimeZone,

    /**
     * Start date time has changed
     */
    StartDate,

    /**
     * End date time has changed
     */
    EndDate,

    /**
     * Title has changed
     */
    Title,

    /**
     * Description has changed
     */
    Description,

    /**
     * Attributes have changed
     */
    Attributes,

    /**
     * Override date time has changed
     */
    OverrideDateTime,

    /**
     * Cancelled flag has changed
     */
    CancelledFlag,

    /**
     * Scheduled meetings flags have changed
     */
    ScheduledMeetingsFlags,

    /**
     * Repetition rules have changed
     */
    RepetitionRules,

    /**
     * Scheduled meetings flags size have changed
     */
    ScheduledMeetingFlagsSize
}