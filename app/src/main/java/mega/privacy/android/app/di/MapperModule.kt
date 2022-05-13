package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.UserUpdateMapper
import mega.privacy.android.app.data.mapper.mapMegaUserListToUserUpdate

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
class MapperModule {

    @Provides
    fun provideUserUpdateMapper(): UserUpdateMapper = ::mapMegaUserListToUserUpdate

}