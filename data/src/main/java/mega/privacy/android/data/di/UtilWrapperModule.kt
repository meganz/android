package mega.privacy.android.data.di

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.DocumentFileFacade
import mega.privacy.android.data.wrapper.BitmapFactoryWrapper
import mega.privacy.android.data.wrapper.DocumentFileWrapper

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UtilWrapperModule {

    /**
     * Provides the default implementation to [DocumentFileWrapper]
     *
     * @param implementation [DocumentFileFacade]
     *
     * @return [DocumentFileWrapper]
     */
    @Binds
    abstract fun bindDocumentFileWrapper(implementation: DocumentFileFacade): DocumentFileWrapper

    companion object {
        /**
         * Provides the [BitmapFactoryWrapper]
         */
        @Provides
        internal fun provideBitmapFactoryWrapper() = object : BitmapFactoryWrapper {
            override fun decodeFile(pathName: String?, opts: BitmapFactory.Options): Bitmap? =
                BitmapFactory.decodeFile(pathName, opts)
        }
    }
}