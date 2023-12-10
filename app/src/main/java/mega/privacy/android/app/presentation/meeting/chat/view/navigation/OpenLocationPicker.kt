package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import mega.privacy.android.app.main.megachat.MapsActivity

internal fun openLocationPicker(
    context: Context,
    locationLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, MapsActivity::class.java).also {
        locationLauncher.launch(it)
    }
}