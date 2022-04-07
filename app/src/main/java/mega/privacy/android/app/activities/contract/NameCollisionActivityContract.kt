package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.MESSAGE_RESULT
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.getIntentForList
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.getIntentForSingleItem
import mega.privacy.android.app.namecollision.data.NameCollision

/**
 * A contract to start NameCollisionActivity and manage its result.
 */
class NameCollisionActivityContract : ActivityResultContract<ArrayList<NameCollision>, String?>() {

    @Suppress("UNCHECKED_CAST")
    override fun createIntent(context: Context, list: ArrayList<NameCollision>): Intent =
        if (list.size == 1) {
            getIntentForSingleItem(context, list[0])
        } else {
            getIntentForList(context, list)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        when (resultCode) {
            Activity.RESULT_OK -> intent?.getStringExtra(MESSAGE_RESULT)
            else -> null
        }
}