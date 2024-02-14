package mega.privacy.android.app.presentation.meeting.chat.view.sheet.options

import mega.privacy.android.core.R as CoreResources
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile

@Composable
internal fun ForwardBottomSheetOption(onClick: () -> Unit) {
    MenuActionListTile(
        text = stringResource(id = R.string.forward_menu_item),
        icon = painterResource(id = CoreResources.drawable.ic_arrow_corner_right),
        modifier = Modifier
            .testTag(CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG)
            .clickable { onClick() }
    )
}

internal const val CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG =
    "chat_message_options_sheet:action_forward"