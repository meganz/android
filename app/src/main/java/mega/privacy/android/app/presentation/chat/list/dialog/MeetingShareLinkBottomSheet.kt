package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.app.presentation.chat.list.view.MeetingLinkView

/**
 * Compose replacement for `MeetingShareLinkBottomSheetFragment`.
 *
 * @param onSendLinkToChat Called when the user picks "Send to chat".
 * @param onShareLink Called when the user picks "Share".
 * @param onDismissRequest Called when the bottom sheet should be dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingShareLinkBottomSheet(
    onSendLinkToChat: () -> Unit,
    onShareLink: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.PartiallyExpanded },
    )

    MegaModalBottomSheet(
        sheetState = sheetState,
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissRequest,
    ) {
        MeetingLinkView(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onSendLinkToChat = {
                onSendLinkToChat()
                onDismissRequest()
            },
            onShareLink = {
                onShareLink()
                onDismissRequest()
            },
        )
    }
}
