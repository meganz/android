package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.DefaultGetTicker
import mega.privacy.android.domain.usecase.DefaultTrackPlaybackPosition
import mega.privacy.android.domain.usecase.GetTicker
import mega.privacy.android.domain.usecase.MonitorPlaybackTimes
import mega.privacy.android.domain.usecase.SavePlaybackTimes
import mega.privacy.android.domain.usecase.TrackPlaybackPosition

/**
 * MediaPlayer use cases module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaPlayerUseCases {

    /**
     * Provide implementation for [TrackPlaybackPosition]
     */
    @Binds
    abstract fun bindTrackPlaybackPosition(implementation: DefaultTrackPlaybackPosition): TrackPlaybackPosition

    /**
     * Provide implementation for [GetTicker]
     */
    @Binds
    abstract fun bindGetTicker(implementation: DefaultGetTicker): GetTicker

    companion object {

        /**
         * Provide implementation for [MonitorPlaybackTimes]
         */
        @Provides
        fun provideMonitorPlaybackTimes(mediaPlayerRepository: MediaPlayerRepository): MonitorPlaybackTimes =
            MonitorPlaybackTimes(mediaPlayerRepository::monitorPlaybackTimes)

        /**
         * Proved implementation for [SavePlaybackTimes]
         */
        @Provides
        fun provideSavePlaybackTimes(mediaPlayerRepository: MediaPlayerRepository): SavePlaybackTimes =
            SavePlaybackTimes(mediaPlayerRepository::savePlaybackTimes)
    }
}