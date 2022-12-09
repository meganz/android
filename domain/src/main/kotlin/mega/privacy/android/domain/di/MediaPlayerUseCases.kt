package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetPlayingPositionHistories
import mega.privacy.android.domain.usecase.SavePlayingPositionHistories

/**
 * MediaPlayer use cases module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaPlayerUseCases {
    companion object {

        /**
         * Provide implementation for [SavePlayingPositionHistories]
         */
        @Provides
        fun provideSavePlayingPositionHistories(settingsRepository: SettingsRepository): SavePlayingPositionHistories =
            SavePlayingPositionHistories(settingsRepository::setStringPreference)

        /**
         * Provide implementation for [GetPlayingPositionHistories]
         */
        @Provides
        fun provideGetPlayingPositionHistories(settingsRepository: SettingsRepository): GetPlayingPositionHistories =
            GetPlayingPositionHistories(settingsRepository::monitorStringPreference)
    }
}