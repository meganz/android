package mega.privacy.android.app.presentation.meeting.chat.view.sheet


import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.chat.messages.reaction.ReactionsInfoView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReactionUser

/**
 * Bottom sheet for reactions for a message
 * @param selectedReaction the reaction user selected and long pressed
 * @param chatId the chat id
 * @param msgId the message id
 * @param modifier
 * @param sheetState the state of the bottom sheet
 * @param onUserClick the callback when a certain user is clicked
 * @param getDetailsInReactionList the function to fill the [UIReactionUser] info
 *
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReactionsInfoBottomSheet(
    selectedReaction: String,
    reactions: List<UIReaction>,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    onUserClick: (Long) -> Unit = {},
    getDetailsInReactionList: suspend (List<UIReaction>) -> List<UIReaction> = { emptyList() },
) {
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }
    }

    val reactionList = produceState(initialValue = reactions) {
        coroutineScope.launch {
            runCatching {
                value = getDetailsInReactionList(reactions)
            }
        }
    }

    ReactionsInfoView(
        modifier = modifier,
        currentReaction = selectedReaction,
        reactionList = reactionList.value,
        onUserClick = onUserClick,
    )
}
