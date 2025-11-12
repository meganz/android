package mega.privacy.android.app.presentation.photos

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey

class SelectAlbumCoverContract :
    ActivityResultContract<LegacyAlbumCoverSelectionNavKey, String?>() {

    override fun createIntent(
        context: Context,
        input: LegacyAlbumCoverSelectionNavKey,
    ): Intent =
        AlbumScreenWrapperActivity
            .createAlbumCoverSelectionScreen(
                context = context,
                albumId = AlbumId(input.albumId),
            )

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return intent.getStringExtra("message").orEmpty()
    }
}