package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.core.ui.controls.chat.messages.ChatRichLinkMessage
import mega.privacy.android.domain.entity.chat.RichPreview

@Composable
fun ChatRichLinkMessageView(
    isMe: Boolean,
    preview: RichPreview?,
    modifier: Modifier = Modifier,
    viewModel: MetaViewModel = hiltViewModel(),
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
            url = preview.url,
            host = preview.domainName,
            image = image.value?.asImageBitmap(),
            icon = icon.value?.asImageBitmap(),
        )
    }
}