package mega.privacy.android.app.presentation.meeting.chat.view.message

import mega.privacy.android.shared.original.core.ui.controls.chat.messages.TimeHeader as CoreTimeHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ManagementMessageViewModel

/**
 * Time header
 *
 * @param timeString
 * @param displayAsMine
 * @param userHandle
 * @param shouldShowName
 * @param modifier
 * @param viewModel
 */
@Composable
fun TimeHeader(
    timeString: String,
    displayAsMine: Boolean,
    userHandle: Long,
    shouldShowName: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ManagementMessageViewModel = hiltViewModel(),
) {
    var userName: String? by remember {
        mutableStateOf(null)
    }
    LaunchedEffect(Unit) {
        if (shouldShowName) {
            userName = viewModel.getParticipantFullName(userHandle)
        }
    }
    CoreTimeHeader(
        timeString = timeString,
        displayAsMine = displayAsMine,
        userName = userName,
        modifier = modifier
    )
}
