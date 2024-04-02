package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.presentation.meeting.chat.view.message.getMessageText
import mega.privacy.android.core.ui.controls.chat.messages.ChatRichLinkMessage
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
    modifier: Modifier = Modifier,
    viewModel: MetaViewModel = hiltViewModel(),
) {
    with(message) {
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
                content = getMessageText(message = content, isEdited = isEdited),
                host = preview.domainName,
                image = image.value?.let { rememberAsyncImagePainter(it) },
                icon = icon.value?.let { rememberAsyncImagePainter(it) },
            )
        }
    }
}