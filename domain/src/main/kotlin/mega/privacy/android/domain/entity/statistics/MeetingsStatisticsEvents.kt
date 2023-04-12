package mega.privacy.android.domain.entity.statistics

/**
 * Events for Meetings statistics
 *
 * @param id Event type or ID
 * @param message Event message
 */
sealed class MeetingsStatisticsEvents(val id: Int, val message: String)

/**
 * Meeting Enable Sound Notification Event
 * User enables sound notification when someone joins or leaves a meeting
 */
class EnableSoundNotification : MeetingsStatisticsEvents(99203, "Meeting Enable Sound Notification")

/**
 * Meeting Disable Sound Notification Event
 * User disables sound notification when someone joins or leaves a meeting
 */
class DisableSoundNotification :
    MeetingsStatisticsEvents(99204, "Meeting Disable Sound Notification")

/**
 * Meeting Stay On Call When Empty Call Scenario
 * User chooses "Stay on call" option when they are the only participant (empty meeting)
 */
class StayOnCallEmptyCall :
    MeetingsStatisticsEvents(99205, "Meeting Stay On Call When Empty Call Scenario")

/**
 * Meeting End Call When Empty Call Scenario
 * User chooses "End call now" option when they are the only participant (empty meeting)
 */
class EndCallEmptyCall :
    MeetingsStatisticsEvents(99206, "Meeting End Call When Empty Call Scenario")

/**
 * Meeting End Call For All Tapped
 * User chooses "End for all" option when leaving a call on which they are host (moderator)
 */
class EndCallForAll : MeetingsStatisticsEvents(99207, "Meeting End Call For All Tapped")

/**
 * Meeting Ended When Empty Call Timeout
 * Call is ended by the timeout when the user is the only participant (empty meeting)
 */
class EndedEmptyCallTimeout :
    MeetingsStatisticsEvents(99208, "Meeting Ended When Empty Call Timeout")