package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader

/**
 * Bottom sheet for not sent
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageNotSentBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    actions: List<@Composable () -> Unit>,
) {
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = sheetState.isVisible) {
        coroutineScope.launch {
            sheetState.hide()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_SEND_ERROR_PANEL)
    ) {
        MenuActionHeader(
            modifier = Modifier.testTag(TEST_TAG_SEND_ERROR_HEADER),
            text = stringResource(id = R.string.title_message_not_sent_options)
        )
        actions.forEach { it() }
    }

}

internal const val TEST_TAG_SEND_ERROR_PANEL = "chat_view:send_error_sheet"
internal const val TEST_TAG_SEND_ERROR_HEADER = "chat_view:send_error_header"