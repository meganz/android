package mega.privacy.android.app.di.avatar

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapperImpl

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
