package mega.privacy.android.app.presentation.meeting.model

/**
 * In meeting UI state
 *
 * @property error                  String resource id for showing an error.
 * @property resultSetOpenInvite    True if it's enabled, false if not.
 */
data class InMeetingState(
    val error: Int? = null,
    val resultSetOpenInvite: Boolean? = null,
)