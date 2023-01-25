package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * View Type Module
 */
@Module(includes = [InternalViewTypeModule::class])
@InstallIn(SingletonComponent::class)
abstract class ViewTypeModule