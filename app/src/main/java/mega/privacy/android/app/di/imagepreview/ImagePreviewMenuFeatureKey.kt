package mega.privacy.android.app.di.imagepreview

import dagger.MapKey
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource

@MapKey
annotation class ImagePreviewMenuFeatureKey(val source: ImagePreviewMenuSource)