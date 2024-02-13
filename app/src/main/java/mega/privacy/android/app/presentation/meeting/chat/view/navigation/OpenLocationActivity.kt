package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.presentation.extensions.canBeHandled
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo

internal fun openLocationActivity(
    context: Context,
    chatGeolocationInfo: ChatGeolocationInfo,
    onNoIntentAvailable: () -> Unit,
) {
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:${chatGeolocationInfo.latitude},${chatGeolocationInfo.longitude}?q=${chatGeolocationInfo.latitude},${chatGeolocationInfo.longitude}")
    ).also {
        if (it.canBeHandled(context)) {
            context.startActivity(it)
        } else {
            onNoIntentAvailable()
        }
    }
}


