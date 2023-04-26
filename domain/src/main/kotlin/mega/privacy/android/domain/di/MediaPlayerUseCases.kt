package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.DefaultGetAudioNodesByParentHandle
import mega.privacy.android.domain.usecase.DefaultGetAudiosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.DefaultGetNodesByHandles
import mega.privacy.android.domain.usecase.DefaultGetTicker
import mega.privacy.android.domain.usecase.DefaultGetVideoNodesByParentHandle
import mega.privacy.android.domain.usecase.DefaultGetVideosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetAudioNodesByParentHandle
import mega.privacy.android.domain.usecase.GetAudiosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandle
import mega.privacy.android.domain.usecase.GetNodesByHandles
import mega.privacy.android.domain.usecase.GetTicker
import mega.privacy.android.domain.usecase.GetVideoNodesByParentHandle
import mega.privacy.android.domain.usecase.GetVideosByParentHandleFromMegaApiFolder

/**
 * MediaPlayer use cases module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaPlayerUseCases {

    /**
     * Provide implementation for [GetTicker]
     */
    @Binds
    abstract fun bindGetTicker(implementation: DefaultGetTicker): GetTicker

    /**
     * Provide implementation for [GetAudioNodesByParentHandle]
     */
    @Binds
    abstract fun bindGetAudioNodesByParentHandle(implementation: DefaultGetAudioNodesByParentHandle): GetAudioNodesByParentHandle

    /**
     * Provide implementation for [GetVideoNodesByParentHandle]
     */
    @Binds
    abstract fun bindGetVideoNodesByParentHandle(implementation: DefaultGetVideoNodesByParentHandle): GetVideoNodesByParentHandle

    /**
     * Provide implementation for [GetAudiosByParentHandleFromMegaApiFolder]
     */
    @Binds
    abstract fun bindGetAudiosByParentHandleFromMegaApiFolder(
        implementation: DefaultGetAudiosByParentHandleFromMegaApiFolder,
    ): GetAudiosByParentHandleFromMegaApiFolder

    /**
     * Provide implementation for [GetVideosByParentHandleFromMegaApiFolder]
     */
    @Binds
    abstract fun bindGetVideosByParentHandleFromMegaApiFolder(
        implementation: DefaultGetVideosByParentHandleFromMegaApiFolder,
    ): GetVideosByParentHandleFromMegaApiFolder

    /**
     * Provide implementation for [GetNodesByHandles]
     */
    @Binds
    abstract fun bindGetNodesByHandles(implementation: DefaultGetNodesByHandles): GetNodesByHandles

    companion object {
        /**
         * Provide implementation for [GetFileUrlByNodeHandle]
         */
        @Provides
        fun provideHttpServerGetLocalLink(mediaPlayerRepository: MediaPlayerRepository): GetFileUrlByNodeHandle =
            GetFileUrlByNodeHandle(mediaPlayerRepository::getFileUrlByNodeHandle)
    }
}