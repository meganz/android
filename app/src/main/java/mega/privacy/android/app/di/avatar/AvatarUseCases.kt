package mega.privacy.android.app.di.avatar

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.usecase.GetMyAvatarColor
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile

/**
 * Provides the use case implementation for avatar
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class AvatarUseCases {
    companion object {
        /**
         * Provide the GetUserAvatarColor implementation
         */
        @Provides
        fun provideGetMyAvatarColor(repository: AvatarRepository): GetMyAvatarColor =
            GetMyAvatarColor(repository::getMyAvatarColor)

        /**
         * Get monitor my avatar file
         */
        @Provides
        fun getMonitorMyAvatarFile(repository: AvatarRepository): MonitorMyAvatarFile =
            MonitorMyAvatarFile(repository::monitorMyAvatarFile)
    }
}