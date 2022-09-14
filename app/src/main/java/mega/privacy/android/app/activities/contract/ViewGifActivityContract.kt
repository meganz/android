package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.activities.GiphyPickerActivity
import mega.privacy.android.app.activities.GiphyViewerActivity
import mega.privacy.android.app.objects.GifData

class ViewGifActivityContract : ActivityResultContract<GifData, GifData?>() {

    override fun createIntent(context: Context, input: GifData): Intent =
        Intent(context, GiphyViewerActivity::class.java)
            .putExtra(GiphyPickerActivity.GIF_DATA, input)

    override fun parseResult(resultCode: Int, intent: Intent?): GifData? =
        when (resultCode) {
            Activity.RESULT_OK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(GiphyPickerActivity.GIF_DATA, GifData::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(GiphyPickerActivity.GIF_DATA)
            }
            else -> null
        }
}
