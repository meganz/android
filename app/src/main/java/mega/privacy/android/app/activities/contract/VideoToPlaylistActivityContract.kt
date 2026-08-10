package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistActivity
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistActivity.Companion.INTENT_ADDED_VIDEO_HANDLE
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistActivity.Companion.INTENT_RESULT_IS_RETRY
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistActivity.Companion.INTENT_RESULT_MESSAGE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult

class VideoToPlaylistActivityContract : ActivityResultContract<Long, AddVideoToPlaylistResult?>() {
    override fun createIntent(context: Context, input: Long): Intent =
        Intent(context, VideoToPlaylistActivity::class.java)
            .putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, input)

    override fun parseResult(resultCode: Int, intent: Intent?): AddVideoToPlaylistResult? =
        when (resultCode) {
            Activity.RESULT_OK -> {
                val message = intent?.getStringExtra(INTENT_RESULT_MESSAGE) ?: ""
                val isRetry = intent?.getBooleanExtra(INTENT_RESULT_IS_RETRY, false) ?: false
                val videoHandle = intent?.getLongExtra(INTENT_ADDED_VIDEO_HANDLE, -1) ?: -1
                AddVideoToPlaylistResult(
                    message = message,
                    isRetry = isRetry,
                    videoHandle = videoHandle
                )
            }

            else -> null
        }
}