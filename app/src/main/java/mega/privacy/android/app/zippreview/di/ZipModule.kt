package mega.privacy.android.app.zippreview.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.zippreview.domain.IZipFileRepo
import mega.privacy.android.app.zippreview.domain.ZipFileRepo

/**
 * Zip module for injection
 */
@Module
@InstallIn(SingletonComponent::class)
class ZipModule {

    @Provides
    fun provideZipFileRepo(): IZipFileRepo {
        return ZipFileRepo()
    }
}