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
import mega.privacy.android.domain.usecase.MegaApiFolderHttpServerIsRunning
import mega.privacy.android.domain.usecase.MegaApiFolderHttpServerSetMaxBufferSize
import mega.privacy.android.domain.usecase.MegaApiFolderHttpServerStart
import mega.privacy.android.domain.usecase.MegaApiFolderHttpServerStop
import mega.privacy.android.domain.usecase.MegaApiHttpServerIsRunning
import mega.privacy.android.domain.usecase.MegaApiHttpServerSetMaxBufferSize
import mega.privacy.android.domain.usecase.MegaApiHttpServerStart
import mega.privacy.android.domain.usecase.MegaApiHttpServerStop
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

        /**
         * Provide implementation for [MegaApiHttpServerStop]
         */
        @Provides
        fun provideMegaApiHttpServerStop(mediaPlayerRepository: MediaPlayerRepository): MegaApiHttpServerStop =
            MegaApiHttpServerStop(mediaPlayerRepository::megaApiHttpServerStop)

        /**
         * Provide implementation for [MegaApiFolderHttpServerStop]
         */
        @Provides
        fun provideMegaApiFolderHttpServerStop(mediaPlayerRepository: MediaPlayerRepository): MegaApiFolderHttpServerStop =
            MegaApiFolderHttpServerStop(mediaPlayerRepository::megaApiFolderHttpServerStop)

        /**
         * Provide implementation for [MegaApiFolderHttpServerIsRunning]
         */
        @Provides
        fun provideMegaApiFolderHttpServerIsRunning(mediaPlayerRepository: MediaPlayerRepository): MegaApiFolderHttpServerIsRunning =
            MegaApiFolderHttpServerIsRunning(mediaPlayerRepository::megaApiFolderHttpServerIsRunning)

        /**
         * Provide implementation for [MegaApiFolderHttpServerStart]
         */
        @Provides
        fun provideMegaApiFolderHttpServerStart(mediaPlayerRepository: MediaPlayerRepository): MegaApiFolderHttpServerStart =
            MegaApiFolderHttpServerStart(mediaPlayerRepository::megaApiFolderHttpServerStart)

        /**
         * Provide implementation for [MegaApiFolderHttpServerSetMaxBufferSize]
         */
        @Provides
        fun provideMegaApiFolderHttpServerSetMaxBufferSize(mediaPlayerRepository: MediaPlayerRepository):
                MegaApiFolderHttpServerSetMaxBufferSize =
            MegaApiFolderHttpServerSetMaxBufferSize(mediaPlayerRepository::megaApiFolderHttpServerSetMaxBufferSize)

        /**
         * Provide implementation for [MegaApiFolderHttpServerSetMaxBufferSize]
         */
        @Provides
        fun provideMegaApiHttpServerSetMaxBufferSize(mediaPlayerRepository: MediaPlayerRepository): MegaApiHttpServerSetMaxBufferSize =
            MegaApiHttpServerSetMaxBufferSize(mediaPlayerRepository::megaApiHttpServerSetMaxBufferSize)

        /**
         * Provide implementation for [MegaApiHttpServerIsRunning]
         */
        @Provides
        fun provideMegaApiHttpServerIsRunning(mediaPlayerRepository: MediaPlayerRepository): MegaApiHttpServerIsRunning =
            MegaApiHttpServerIsRunning(mediaPlayerRepository::megaApiHttpServerIsRunning)

        /**
         * Provide implementation for [MegaApiHttpServerStart]
         */
        @Provides
        fun provideMegaApiHttpServerStart(mediaPlayerRepository: MediaPlayerRepository): MegaApiHttpServerStart =
            MegaApiHttpServerStart(mediaPlayerRepository::megaApiHttpServerStart)
    }
}