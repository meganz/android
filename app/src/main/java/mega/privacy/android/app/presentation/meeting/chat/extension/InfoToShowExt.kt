package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow

/**
 * Extension function to get the info to show.
 */
fun InfoToShow.getInfo(context: Context) = with(this) {
    when (this) {
        is InfoToShow.InviteContactResult -> result.toInfoText(context)
        is InfoToShow.MuteOptionResult -> result.toInfoText(context)
        is InfoToShow.StringWithParams -> context.getString(stringId, *args.toTypedArray())
        is InfoToShow.SimpleString -> context.getString(stringId)
        is InfoToShow.QuantityString -> context.resources.getQuantityString(stringId, count, count)
        is InfoToShow.ForwardMessagesResult -> result.toInfoText(context)
    }
}