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
 * [Any] must be ArrayList<NameCollision> or [NameCollision].
 */
class NameCollisionActivityContract : ActivityResultContract<Any, String?>() {

    @Suppress("UNCHECKED_CAST")
    override fun createIntent(context: Context, any: Any): Intent =
        when (any) {
            is ArrayList<*> -> getIntentForList(context, any as ArrayList<NameCollision>)
            else -> getIntentForSingleItem(context, any as NameCollision)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        when (resultCode) {
            Activity.RESULT_OK -> intent?.getStringExtra(MESSAGE_RESULT)
            else -> null
        }
}