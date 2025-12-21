package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity

class AddToAlbumActivityContract : ActivityResultContract<Pair<Array<Long>, Int>, String?>() {

    override fun createIntent(context: Context, input: Pair<Array<Long>, Int>): Intent =
        Intent(context, AddToAlbumActivity::class.java).apply {
            val ids = input.first
            putExtra("ids", ids)
            putExtra("type", input.second)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        if (resultCode != Activity.RESULT_OK) null else {
            intent?.getStringExtra("message")
        }
}