package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapper
import mega.privacy.android.app.presentation.manager.model.mapper.InitialScreenMapperImpl

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
class MapperModule {

    /**
     * Provide initial screen mapper
     */
    @Provides
    fun provideInitialScreenMapper(): InitialScreenMapper = InitialScreenMapperImpl()
}