package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.core.ui.controls.chat.messages.ChatRichLinkMessage
import mega.privacy.android.domain.entity.chat.messages.meta.ChatRichPreviewInfo

/**
 * Chat rich link message view
 *
 * @param isMe
 * @param preview
 * @param content
 * @param modifier
 * @param viewModel
 */
@Composable
fun ChatRichLinkMessageView(
    isMe: Boolean,
    preview: ChatRichPreviewInfo?,
    content: String,
    modifier: Modifier = Modifier,
    viewModel: MetaViewModel = hiltViewModel(),
    isEdited: Boolean,
) {
    preview?.let {
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
            isMe = isMe,
            title = preview.title,
            contentTitle = preview.description,
            contentDescription = preview.description,
            content = content,
            host = preview.domainName,
            image = image.value?.let { rememberAsyncImagePainter(it) },
            icon = icon.value?.let { rememberAsyncImagePainter(it) },
        )
    }
}