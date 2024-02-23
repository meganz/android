package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import mega.privacy.android.app.main.megachat.MapsActivity

internal fun openLocationPicker(
    context: Context,
    locationLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    msgId: Long = INVALID_LOCATION_MESSAGE_ID,
) {
    Intent(context, MapsActivity::class.java).also {
        msgId.takeIf { id -> id != INVALID_LOCATION_MESSAGE_ID }?.let { id ->
            it.putExtra(MapsActivity.EDITING_MESSAGE, true)
            it.putExtra(MapsActivity.MSG_ID, id)
        }
        it.putExtra(MapsActivity.MSG_ID, msgId)
        locationLauncher.launch(it)
    }
}

internal const val INVALID_LOCATION_MESSAGE_ID = -1L