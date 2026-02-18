package mega.privacy.android.app.fetcher

import coil3.key.Keyer
import coil3.request.Options
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest

internal object MediaThumbnailKeyer : Keyer<MediaThumbnailRequest> {

    override fun key(data: MediaThumbnailRequest, options: Options): String =
        "${data.id}-${options.size}"
}
