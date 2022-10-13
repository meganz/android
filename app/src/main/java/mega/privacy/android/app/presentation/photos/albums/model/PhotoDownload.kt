package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Photo

typealias PhotoDownload = suspend (
    isPreview: Boolean,
    photo: Photo,
    callback: (success: Boolean) -> Unit,
) -> Unit