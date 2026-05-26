package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import mega.privacy.android.app.components.largebundle.largeBundleHolder
import mega.privacy.android.app.namecollision.NameCollisionActivity
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.app.utils.Constants

internal fun openNameCollisionActivity(
    context: Context,
    collisions: List<NameCollisionUiEntity>,
    launcher: ActivityResultLauncher<Intent>,
) {
    val intent =
        Intent(context, NameCollisionActivity::class.java).apply {
            if (collisions.size == 1) {
                putExtra(Constants.INTENT_EXTRA_SINGLE_COLLISION_RESULT, collisions.first())
            } else {
                val key = context.largeBundleHolder.put(
                    bundleOf(
                        Constants.INTENT_EXTRA_COLLISION_RESULTS to ArrayList(collisions)
                    )
                )
                putExtra(NameCollisionActivity.EXTRA_COLLISIONS_KEY, key)
            }
        }

    launcher.launch(intent)
}