package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.meeting.CallUIStatusType

/**
 * In meeting UI state
 *
 * @property error                  String resource id for showing an error.
 * @property resultSetOpenInvite    True if it's enabled, false if not.
 * @property callUIStatus           [CallUIStatusType]
 */
data class InMeetingState(
    val error: Int? = null,
    val resultSetOpenInvite: Boolean? = null,
    var callUIStatus: CallUIStatusType = CallUIStatusType.None
)