package mega.privacy.android.domain.entity.statistics

/**
 * Events for data statistics regarding media player
 *
 * @property id Event type or ID
 * @property message Event message
 * @property addJourneyId True if JourneyID should be included. Otherwise, false.
 * @property viewId ViewID value (C-string null-terminated) to be sent with the event.
 *                     This value should have been generated with [this.generateViewId].
 */
sealed class MediaPlayerStatisticsEvents(
    val id: Int,
    val message: String,
    val addJourneyId: Boolean = false,
    val viewId: String? = null,
) {

    /**
     * The Video Player is Activated Event
     * The video file starts to be playing in media player page.
     */
    class VideoPlayerActivatedEvent :
        MediaPlayerStatisticsEvents(99209, "The Video Player is Activated")

    /**
     * The Subtitle Dialog is Shown Event
     * The user clicked the subtitle button and adding subtitle dialog is enabled.
     */
    class SubtitleDialogShownEvent :
        MediaPlayerStatisticsEvents(99210, "Subtitle Dialog is Shown")

    /**
     * Hide Subtitle Event
     * The user clicked the subtitle button and hide the subtitle
     */
    class HideSubtitleEvent : MediaPlayerStatisticsEvents(99211, "Hide Subtitle")

    /**
     * Auto Match Subtitle Option is Clicked Event
     * The user clicked the auto-match subtitle option, if there is an auto-match subtitle file
     */
    class AutoMatchSubtitleClickedEvent :
        MediaPlayerStatisticsEvents(99212, "Auto Match Subtitle Option is Clicked")

    /**
     * Open Select Subtitle Page Event
     * The user clicked the "Add subtitle..." option and open select subtitle page
     */
    class OpenSelectSubtitlePageEvent :
        MediaPlayerStatisticsEvents(99213, "Open Select Subtitle Page")

    /**
     * Search Mode is Enabled Event
     * The search mode of select subtitle page is enabled
     */
    class SearchModeEnabledEvent : MediaPlayerStatisticsEvents(99214, "Search Mode is Enabled")

    /**
     * Add Subtitle Button is Clicked Event
     * The user clicked the add subtitle button of select subtitle page
     */
    class AddSubtitleClickedEvent :
        MediaPlayerStatisticsEvents(99215, "Add Subtitle Button is Clicked")

    /**
     * Select Subtitle is Cancelled Event
     * The user cancelled select subtitle
     */
    class SelectSubtitleCancelledEvent :
        MediaPlayerStatisticsEvents(99216, "Select Subtitle is Cancelled")

    /**
     * Loop Button is Enabled Event
     * The user enabled the loop feature
     */
    class LoopButtonEnabledEvent : MediaPlayerStatisticsEvents(99217, "Loop Button is Enabled")

    /**
     * Screen is Locked Event
     * The user locked the screen when playing video
     */
    class ScreenLockedEvent : MediaPlayerStatisticsEvents(99218, "Screen is Locked")

    /**
     * Screen is Unlocked Event
     * The user unlocked the screen when playing video
     */
    class ScreenUnlockedEvent : MediaPlayerStatisticsEvents(99219, "Screen is Unlocked")

    /**
     * Snapshot Button isClicked Event
     * The user clicked the Snapshot button
     */
    class SnapshotButtonClickedEvent :
        MediaPlayerStatisticsEvents(99220, "Snapshot Button is Clicked")

    /**
     * Info Button is Clicked Event
     * The user clicked the info button
     */
    class InfoButtonClickedEvent : MediaPlayerStatisticsEvents(99221, "Info Button is Clicked")

    /**
     * Save to Device Button is Clicked Event
     * The user clicked the save to device button
     */
    class SaveToDeviceButtonClickedEvent :
        MediaPlayerStatisticsEvents(99222, "Save to Device Button is Clicked")

    /**
     * Send to Chat Button is Clicked Event
     * The use clicked the send to chat button
     */
    class SendToChatButtonClickedEvent :
        MediaPlayerStatisticsEvents(99223, "Send to Chat Button is Clicked")

    /**
     * Share Button is Clicked Button
     * The use clicked the share button
     */
    class ShareButtonClickedEvent :
        MediaPlayerStatisticsEvents(99224, "Share Button is Clicked")

    /**
     * Get Link Button is Clicked Button
     * The use clicked the get link button
     */
    class GetLinkButtonClickedEvent :
        MediaPlayerStatisticsEvents(99225, "Get Link Button is Clicked")

    /**
     * Remove Link Button is Clicked Button
     * The use clicked the remove link button
     */
    class RemoveLinkButtonClickedEvent :
        MediaPlayerStatisticsEvents(99226, "Remove Link Button is Clicked")
}