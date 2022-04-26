package mega.privacy.android.app.zippreview.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.zippreview.domain.DefaultZipFileRepository
import mega.privacy.android.app.zippreview.domain.ZipFileRepository

/**
 * Zip module for injection
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ZipModule {

    @Binds
    abstract fun bindZipFileRepo(zipFileRepo: DefaultZipFileRepository): ZipFileRepository
}