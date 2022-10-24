package mega.privacy.android.data.di

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.wrapper.BitmapFactoryWrapper

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UtilWrapperModule {
    /**
     * provide Bitmap Factory Wrapper
     */
    companion object {
        @Provides
        internal fun provideBitmapFactoryWrapper() = object : BitmapFactoryWrapper {
            override fun decodeFile(pathName: String?, opts: BitmapFactory.Options): Bitmap? =
                BitmapFactory.decodeFile(pathName, opts)
        }
    }
}