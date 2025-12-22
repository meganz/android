package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.namecollision.NameCollisionActivity.Companion.MESSAGE_RESULT
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistActivity
import mega.privacy.android.app.utils.Constants

class VideoToPlaylistActivityContract : ActivityResultContract<Long, List<String>?>() {
    override fun createIntent(context: Context, input: Long): Intent =
        Intent(context, VideoToPlaylistActivity::class.java)
            .putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, input)

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? =
        when (resultCode) {
            Activity.RESULT_OK -> intent?.getStringArrayListExtra(
                VideoToPlaylistActivity.INTENT_SUCCEED_ADDED_PLAYLIST_TITLES
            )

            else -> null
        }
}