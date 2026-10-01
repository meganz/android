package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * OpenFileUseCasesModule
 */
@Module(includes = [InternalOpenFileUseCasesModule::class])
@InstallIn(SingletonComponent::class)
abstract class OpenFileUseCasesModule