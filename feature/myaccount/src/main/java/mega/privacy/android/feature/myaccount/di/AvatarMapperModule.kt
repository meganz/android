package mega.privacy.android.feature.myaccount.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.myaccount.presentation.mapper.AvatarContentMapper
import mega.privacy.android.feature.myaccount.presentation.mapper.AvatarContentMapperImpl

/**
 * DI to provide avatar mapper
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AvatarMapperModule {

    /**
     * Provide the instance of [AvatarContentMapper]
     */
    @Binds
    abstract fun bindAvatarContentMapper(implementation: AvatarContentMapperImpl): AvatarContentMapper
}