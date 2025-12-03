package mega.privacy.android.app.presentation.photos

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.feature.photos.model.AlbumFlow
import mega.privacy.android.navigation.destination.LegacyPhotoSelectionNavKey

class SelectAlbumPhotosContract :
    ActivityResultContract<LegacyPhotoSelectionNavKey, Long?>() {

    override fun createIntent(
        context: Context,
        input: LegacyPhotoSelectionNavKey,
    ): Intent =
        AlbumScreenWrapperActivity
            .createAlbumPhotosSelectionScreen(
                context = context,
                albumId = AlbumId(input.albumId),
                albumFlow = AlbumFlow.entries[input.selectionMode]
            )

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Long? {
        val numPhotos = intent?.getIntExtra(AlbumScreenWrapperActivity.NUM_PHOTOS, 0) ?: 0
        if (numPhotos == 0) return null
        val albumId = intent?.getLongExtra(AlbumScreenWrapperActivity.ALBUM_ID, -1) ?: -1

        return albumId
    }
}