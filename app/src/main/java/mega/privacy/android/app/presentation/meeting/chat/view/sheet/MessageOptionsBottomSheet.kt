package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.chat.MegaEmojiPickerView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.AddReactionsSheetItem
import mega.privacy.android.core.ui.controls.dividers.DividerSpacing
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Bottom sheet for chat message options.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageOptionsBottomSheet(
    showReactionPicker: Boolean,
    onReactionClicked: (String) -> Unit,
    onMoreReactionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
) {
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = sheetState.isVisible) {
        coroutineScope.launch {
            sheetState.hide()
        }
    }

    AnimatedVisibility(visible = !showReactionPicker) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag(TEST_TAG_MESSAGE_OPTIONS_PANEL)
        ) {
            AddReactionsSheetItem(
                onReactionClicked = {
                    onReactionClicked(it)
                },
                onMoreReactionsClicked = onMoreReactionsClicked,
                modifier = Modifier.padding(8.dp),
            )
            MegaDivider(dividerSpacing = DividerSpacing.Full)
        }
    }
    AnimatedVisibility(visible = showReactionPicker) {
        MegaEmojiPickerView(
            onEmojiPicked = {
                //Add reaction
                onReactionClicked(it.emoji)
            },
            showEmojiPicker = showReactionPicker,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun MessageOptionsBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MessageOptionsBottomSheet(
            showReactionPicker = false,
            onReactionClicked = {},
            onMoreReactionsClicked = {})
    }
}

internal const val TEST_TAG_MESSAGE_OPTIONS_PANEL = "chat_view:message_options_panel"