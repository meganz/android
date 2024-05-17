package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextViewModel
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.ChatRichLinkMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage

/**
 * Chat rich link message view
 *
 * @param message
 * @param modifier
 * @param viewModel
 */
@Composable
fun ChatRichLinkMessageView(
    message: RichPreviewMessage,
    interactionEnabled: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MetaViewModel = hiltViewModel(),
    messageTextViewModel: ChatMessageTextViewModel = hiltViewModel(),
) {
    with(message) {
        var links by rememberSaveable { mutableStateOf(emptyList<String>()) }
        LaunchedEffect(Unit) {
            links = messageTextViewModel.getLinks(content)
        }

        chatRichPreviewInfo?.let { preview ->
            val image = produceState<Bitmap?>(initialValue = null) {
                preview.image?.let { bitmapString ->
                    viewModel.getBitmap(bitmapString)?.let { bitmap ->
                        value = bitmap
                    }
                }
            }
            val icon = produceState<Bitmap?>(initialValue = null) {
                preview.icon?.let { bitmapString ->
                    viewModel.getBitmap(bitmapString)?.let { bitmap ->
                        value = bitmap
                    }
                }
            }

            ChatRichLinkMessage(
                modifier = modifier,
                isMine = isMine,
                title = preview.title,
                contentTitle = preview.title,
                contentDescription = preview.description,
                content = content,
                links = links,
                isEdited = isEdited,
                host = preview.domainName,
                image = image.value?.let { rememberAsyncImagePainter(it) },
                icon = icon.value?.let { rememberAsyncImagePainter(it) },
                interactionEnabled = interactionEnabled,
                onLongClick = onLongClick,
            )
        }
    }
}