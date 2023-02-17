package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Folder Link Module
 */
@Module(includes = [InternalFolderLinkModule::class])
@InstallIn(SingletonComponent::class)
abstract class FolderLinkModule