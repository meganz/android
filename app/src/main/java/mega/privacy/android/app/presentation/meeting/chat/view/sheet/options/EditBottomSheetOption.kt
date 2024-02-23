package mega.privacy.android.app.presentation.meeting.chat.view.sheet.options

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile

@Composable
internal fun EditBottomSheetOption(onClick: () -> Unit) {
    MenuActionListTile(
        text = stringResource(id = R.string.title_edit_profile_info),
        icon = painterResource(id = R.drawable.ic_pen_2_medium_regular_outline),
        modifier = Modifier
            .testTag(CHAT_BOTTOM_SHEET_OPTION_EDIT_TAG)
            .clickable { onClick() }
    )
}

internal const val CHAT_BOTTOM_SHEET_OPTION_EDIT_TAG =
    "chat_message_options_sheet:action_edit"