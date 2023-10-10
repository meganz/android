package mega.privacy.android.app.di.imagepreview

import dagger.MapKey
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource

@MapKey
annotation class ImageNodeFetcherSourceKey(val source: ImagePreviewFetcherSource)