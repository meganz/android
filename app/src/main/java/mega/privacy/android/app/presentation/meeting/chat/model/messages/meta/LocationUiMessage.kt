package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.ChatLocationMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openLocationActivity
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Location ui message
 */
class LocationUiMessage(
    override val message: LocationMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
        navHostController: NavHostController,
    ) {
        val context = LocalContext.current
        val snackbarHostState = LocalSnackBarHostState.current
        val coroutineScope = rememberCoroutineScope()
        ChatLocationMessageView(
            message = message,
            isEdited = message.isEdited,
            modifier = initialiseModifier {
                message.chatGeolocationInfo?.let {
                    openLocationActivity(context, it) {
                        coroutineScope.launch {
                            snackbarHostState?.showAutoDurationSnackbar(
                                context.getString(R.string.intent_not_available_location)
                            )
                        }
                    }
                }
            }
        )
    }

    override val modifier: Modifier
        get() = if (message.isMine) {
            Modifier
                .padding(start = 8.dp)
                .fillMaxWidth()
        } else {
            Modifier
                .padding(end = 8.dp)
                .fillMaxWidth()
        }

    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}
