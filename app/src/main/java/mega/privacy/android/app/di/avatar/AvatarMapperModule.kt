package mega.privacy.android.app.di.avatar

import android.graphics.Bitmap
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapperImpl
import mega.privacy.android.app.presentation.avatar.mapper.AvatarMapper
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE

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

    companion object {

        /**
         * Method to provide the avatar mapper
         */
        @Provides
        fun provideAvatarMapper(): AvatarMapper = object : AvatarMapper {
            override suspend fun getDefaultAvatar(
                color: Int,
                text: String,
                isList: Boolean,
            ): Bitmap = AvatarUtil.getDefaultAvatar(color, text, AVATAR_SIZE, isList)
        }
    }
}
