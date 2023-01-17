package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.CredentialsIsNull
import mega.privacy.android.domain.usecase.DefaultGetTicker
import mega.privacy.android.domain.usecase.DefaultTrackPlaybackPosition
import mega.privacy.android.domain.usecase.GetLocalFilePath
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApi
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApi
import mega.privacy.android.domain.usecase.GetThumbnailFromMegaApi
import mega.privacy.android.domain.usecase.GetThumbnailFromMegaApiFolder
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
         * Provide implementation for [GetLocalFolderLinkFromMegaApiFolder]
         */
        @Provides
        fun provideGetLocalLinkFromMegaApiFolder(mediaPlayerRepository: MediaPlayerRepository): GetLocalFolderLinkFromMegaApiFolder =
            GetLocalFolderLinkFromMegaApiFolder(mediaPlayerRepository::getLocalLinkForFolderLinkFromMegaApiFolder)

        /**
         * Provide implementation for [GetLocalFolderLinkFromMegaApi]
         */
        @Provides
        fun provideGetLocalFolderLinkFromMegaApi(mediaPlayerRepository: MediaPlayerRepository): GetLocalFolderLinkFromMegaApi =
            GetLocalFolderLinkFromMegaApi(mediaPlayerRepository::getLocalLinkForFolderLinkFromMegaApi)

        /**
         * Provide implementation for [GetThumbnailFromMegaApiFolder]
         */
        @Provides
        fun provideGetThumbnailFromMegaApiFolder(mediaPlayerRepository: MediaPlayerRepository): GetThumbnailFromMegaApiFolder =
            GetThumbnailFromMegaApiFolder(mediaPlayerRepository::getThumbnailFromMegaApiFolder)

        /**
         * Provide implementation for [GetThumbnailFromMegaApiFolder]
         */
        @Provides
        fun provideGetThumbnailFromMegaApi(mediaPlayerRepository: MediaPlayerRepository): GetThumbnailFromMegaApi =
            GetThumbnailFromMegaApi(mediaPlayerRepository::getThumbnailFromMegaApi)

        /**
         * Provide implementation for [GetLocalFilePath]
         */
        @Provides
        fun provideGetLocalFilePath(mediaPlayerRepository: MediaPlayerRepository): GetLocalFilePath =
            GetLocalFilePath(mediaPlayerRepository::getLocalFilePath)

        /**
         * Provide implementation for [GetLocalLinkFromMegaApi]
         */
        @Provides
        fun provideGetLocalLinkFromMegaApi(mediaPlayerRepository: MediaPlayerRepository): GetLocalLinkFromMegaApi =
            GetLocalLinkFromMegaApi(mediaPlayerRepository::getLocalLinkFromMegaApi)

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
         * Provide implementation for [CredentialsIsNull]
         */
        @Provides
        fun provideCredentialsIsNull(mediaPlayerRepository: MediaPlayerRepository): CredentialsIsNull =
            CredentialsIsNull(mediaPlayerRepository::credentialsIsNull)

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