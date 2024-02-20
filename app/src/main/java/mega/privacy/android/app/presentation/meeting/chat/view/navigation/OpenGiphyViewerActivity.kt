package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.activities.GiphyPickerActivity
import mega.privacy.android.app.activities.GiphyViewerActivity
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo

internal fun openGiphyViewerActivity(
    context: Context,
    gifInfo: ChatGifInfo,
) {
    Intent(context, GiphyViewerActivity::class.java).apply {
        action = Constants.ACTION_PREVIEW_GIPHY
        putExtra(GiphyPickerActivity.GIF_DATA, gifInfo.toGifData())
    }.also { context.startActivity(it) }
}

private fun ChatGifInfo.toGifData() = GifData(
    mp4Src,
    webpSrc,
    mp4Size.toLong(),
    webpSize.toLong(),
    width,
    height,
    title
)