package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import mega.privacy.android.app.namecollision.LegacyNameCollisionActivity
import mega.privacy.android.app.namecollision.data.LegacyNameCollision
import mega.privacy.android.app.utils.Constants

internal fun openNameCollisionActivity(
    context: Context,
    collisions: List<LegacyNameCollision>,
    launcher: ActivityResultLauncher<Intent>,
) {
    val intent =
        Intent(context, LegacyNameCollisionActivity::class.java).apply {
            if (collisions.size == 1) {
                putExtra(Constants.INTENT_EXTRA_SINGLE_COLLISION_RESULT, collisions.first())
            } else {
                putExtra(Constants.INTENT_EXTRA_COLLISION_RESULTS, arrayListOf(collisions))
            }
        }

    launcher.launch(intent)
}