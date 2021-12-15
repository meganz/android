package mega.privacy.android.app.zippreview.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import mega.privacy.android.app.zippreview.domain.IZipFileRepo
import mega.privacy.android.app.zippreview.domain.ZipFileRepo

@Module
@InstallIn(ApplicationComponent::class)
class ZipModule {

    @Provides
    fun provideZipFileRepo(): IZipFileRepo {
        return ZipFileRepo()
    }
}