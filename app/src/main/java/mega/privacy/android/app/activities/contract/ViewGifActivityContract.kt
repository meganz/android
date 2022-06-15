package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
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
            Activity.RESULT_OK -> intent?.getParcelableExtra(GiphyPickerActivity.GIF_DATA) as GifData?
            else -> null
        }
}
