package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.usecase.imageviewer.DefaultGetImageByNodeHandle
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandle

/**
 * Internal module class to provide all Use Cases for ImageViewer
 */
@Module
@DisableInstallInCheck
internal abstract class ImageViewerModule {
    @Binds
    abstract fun bindGetImageByNodeHandle(implementation: DefaultGetImageByNodeHandle): GetImageByNodeHandle
}