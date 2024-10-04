package mega.privacy.android.app.presentation.meeting.view.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon

/**
 * Menu actions for the scheduled meeting info screen
 */
sealed interface ScheduledMeetingInfoMenuAction : MenuAction {

    /**
     * Add participants action
     */
    data object AddParticipants : MenuActionWithIcon, ScheduledMeetingInfoMenuAction {
        @Composable
        override fun getIconPainter() =
            painterResource(id = R.drawable.add_participants)

        @Composable
        override fun getDescription() = "Add Participants"

        override val testTag: String
            get() = "menu_action:add_participants"
    }

    /**
     * Edit meeting action
     */
    data object EditMeeting : MenuActionWithIcon, ScheduledMeetingInfoMenuAction {
        @Composable
        override fun getIconPainter() =
            painterResource(id = R.drawable.ic_scheduled_meeting_edit)

        @Composable
        override fun getDescription() = "Edit Meeting"

        override val testTag: String
            get() = "menu_action:edit_meeting"
    }
}