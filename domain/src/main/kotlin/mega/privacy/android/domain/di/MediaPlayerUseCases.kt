package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.AreCredentialsNull
import mega.privacy.android.domain.usecase.DefaultGetAudioNodes
import mega.privacy.android.domain.usecase.DefaultGetAudioNodesByEmail
import mega.privacy.android.domain.usecase.DefaultGetAudioNodesByParentHandle
import mega.privacy.android.domain.usecase.DefaultGetAudioNodesFromInShares
import mega.privacy.android.domain.usecase.DefaultGetAudioNodesFromOutShares
import mega.privacy.android.domain.usecase.DefaultGetAudioNodesFromPublicLinks
import mega.privacy.android.domain.usecase.DefaultGetAudiosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.DefaultGetNodesByHandles
import mega.privacy.android.domain.usecase.DefaultGetTicker
import mega.privacy.android.domain.usecase.DefaultGetVideoNodes
import mega.privacy.android.domain.usecase.DefaultGetVideoNodesByEmail
import mega.privacy.android.domain.usecase.DefaultGetVideoNodesByParentHandle
import mega.privacy.android.domain.usecase.DefaultGetVideoNodesFromInShares
import mega.privacy.android.domain.usecase.DefaultGetVideoNodesFromOutShares
import mega.privacy.android.domain.usecase.DefaultGetVideoNodesFromPublicLinks
import mega.privacy.android.domain.usecase.DefaultGetVideosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.DefaultTrackPlaybackPosition
import mega.privacy.android.domain.usecase.DeletePlaybackInformation
import mega.privacy.android.domain.usecase.GetAudioNodes
import mega.privacy.android.domain.usecase.GetAudioNodesByEmail
import mega.privacy.android.domain.usecase.GetAudioNodesByParentHandle
import mega.privacy.android.domain.usecase.GetAudioNodesFromInShares
import mega.privacy.android.domain.usecase.GetAudioNodesFromOutShares
import mega.privacy.android.domain.usecase.GetAudioNodesFromPublicLinks
import mega.privacy.android.domain.usecase.GetAudiosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandle
import mega.privacy.android.domain.usecase.GetInboxNode
import mega.privacy.android.domain.usecase.GetLocalFilePath
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApi
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApi
import mega.privacy.android.domain.usecase.GetNodesByHandles
import mega.privacy.android.domain.usecase.GetParentNodeByHandle
import mega.privacy.android.domain.usecase.GetParentNodeFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetRootNode
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetRubbishNode
import mega.privacy.android.domain.usecase.GetSubtitleFileInfoList
import mega.privacy.android.domain.usecase.GetThumbnailFromMegaApi
import mega.privacy.android.domain.usecase.GetThumbnailFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetTicker
import mega.privacy.android.domain.usecase.GetUnTypedNodeByHandle
import mega.privacy.android.domain.usecase.GetUserNameByEmail
import mega.privacy.android.domain.usecase.GetVideoNodes
import mega.privacy.android.domain.usecase.GetVideoNodesByEmail
import mega.privacy.android.domain.usecase.GetVideoNodesByParentHandle
import mega.privacy.android.domain.usecase.GetVideoNodesFromInShares
import mega.privacy.android.domain.usecase.GetVideoNodesFromOutShares
import mega.privacy.android.domain.usecase.GetVideoNodesFromPublicLinks
import mega.privacy.android.domain.usecase.GetVideosByParentHandleFromMegaApiFolder
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

    /**
     * Provide implementation for [GetAudioNodes]
     */
    @Binds
    abstract fun bindGetAudioNodes(implementation: DefaultGetAudioNodes): GetAudioNodes

    /**
     * Provide implementation for [GetVideoNodes]
     */
    @Binds
    abstract fun bindGetVideoNodes(implementation: DefaultGetVideoNodes): GetVideoNodes

    /**
     * Provide implementation for [GetAudioNodesFromPublicLinks]
     */
    @Binds
    abstract fun bindGetAudioNodesFromPublicLinks(implementation: DefaultGetAudioNodesFromPublicLinks): GetAudioNodesFromPublicLinks

    /**
     * Provide implementation for [GetVideoNodesFromPublicLinks]
     */
    @Binds
    abstract fun bindGetVideoNodesFromPublicLinks(implementation: DefaultGetVideoNodesFromPublicLinks): GetVideoNodesFromPublicLinks

    /**
     * Provide implementation for [GetAudioNodesFromInShares]
     */
    @Binds
    abstract fun bindGetAudioNodesFromInShares(implementation: DefaultGetAudioNodesFromInShares): GetAudioNodesFromInShares

    /**
     * Provide implementation for [GetVideoNodesFromInShares]
     */
    @Binds
    abstract fun bindGetVideoNodesFromInShares(implementation: DefaultGetVideoNodesFromInShares): GetVideoNodesFromInShares

    /**
     * Provide implementation for [GetAudioNodesFromOutShares]
     */
    @Binds
    abstract fun bindGetAudioNodesFromOutShares(implementation: DefaultGetAudioNodesFromOutShares): GetAudioNodesFromOutShares

    /**
     * Provide implementation for [GetVideoNodesFromOutShares]
     */
    @Binds
    abstract fun bindGetVideoNodesFromOutShares(implementation: DefaultGetVideoNodesFromOutShares): GetVideoNodesFromOutShares

    /**
     * Provide implementation for [GetAudioNodesByEmail]
     */
    @Binds
    abstract fun bindGetAudioNodesByEmail(implementation: DefaultGetAudioNodesByEmail): GetAudioNodesByEmail

    /**
     * Provide implementation for [GetVideoNodesByEmail]
     */
    @Binds
    abstract fun bindGetVideoNodesByEmail(implementation: DefaultGetVideoNodesByEmail): GetVideoNodesByEmail

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
         * Provide implementation for [DeletePlaybackInformation]
         */
        @Provides
        fun provideDeletePlaybackInformation(mediaPlayerRepository: MediaPlayerRepository): DeletePlaybackInformation =
            DeletePlaybackInformation(mediaPlayerRepository::deletePlaybackInformation)

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
         * Provide implementation for [GetUserNameByEmail]
         */
        @Provides
        fun provideGetUserNameByEmail(mediaPlayerRepository: MediaPlayerRepository): GetUserNameByEmail =
            GetUserNameByEmail(mediaPlayerRepository::getUserNameByEmail)

        /**
         * Provide implementation for [GetRootNode]
         */
        @Provides
        fun provideGetRootNode(mediaPlayerRepository: MediaPlayerRepository): GetRootNode =
            GetRootNode(mediaPlayerRepository::getRootNode)

        /**
         * Provide implementation for [GetInboxNode]
         */
        @Provides
        fun provideGetInboxNode(mediaPlayerRepository: MediaPlayerRepository): GetInboxNode =
            GetInboxNode(mediaPlayerRepository::getInboxNode)

        /**
         * Provide implementation for [GetRubbishNode]
         */
        @Provides
        fun provideGetRubbishNode(mediaPlayerRepository: MediaPlayerRepository): GetRubbishNode =
            GetRubbishNode(mediaPlayerRepository::getRubbishNode)

        /**
         * Provide implementation for [GetParentNodeByHandle]
         */
        @Provides
        fun provideGetParentNodeByHandle(mediaPlayerRepository: MediaPlayerRepository): GetParentNodeByHandle =
            GetParentNodeByHandle(mediaPlayerRepository::getParentNodeByHandle)

        /**
         * Provide implementation for [GetRootNodeFromMegaApiFolder]
         */
        @Provides
        fun provideGetRootNodeFromMegaApiFolder(mediaPlayerRepository: MediaPlayerRepository): GetRootNodeFromMegaApiFolder =
            GetRootNodeFromMegaApiFolder(mediaPlayerRepository::getRootNodeFromMegaApiFolder)

        /**
         * Provide implementation for [GetParentNodeFromMegaApiFolder]
         */
        @Provides
        fun provideGetParentNodeFromMegaApiFolder(mediaPlayerRepository: MediaPlayerRepository): GetParentNodeFromMegaApiFolder =
            GetParentNodeFromMegaApiFolder(mediaPlayerRepository::getParentNodeFromMegaApiFolder)

        /**
         * Provide implementation for [GetUnTypedNodeByHandle]
         */
        @Provides
        fun provideGetUnTypedNodeByHandle(mediaPlayerRepository: MediaPlayerRepository): GetUnTypedNodeByHandle =
            GetUnTypedNodeByHandle(mediaPlayerRepository::getUnTypedNodeByHandle)

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
         * Provide implementation for [AreCredentialsNull]
         */
        @Provides
        fun provideCredentialsNull(mediaPlayerRepository: MediaPlayerRepository): AreCredentialsNull =
            AreCredentialsNull(mediaPlayerRepository::areCredentialsNull)

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

        /**
         * Provide implementation for [GetFileUrlByNodeHandle]
         */
        @Provides
        fun provideHttpServerGetLocalLink(mediaPlayerRepository: MediaPlayerRepository): GetFileUrlByNodeHandle =
            GetFileUrlByNodeHandle(mediaPlayerRepository::getFileUrlByNodeHandle)

        /**
         * Provide implementation for [GetSubtitleFileInfoList]
         */
        @Provides
        fun provideGetSubtitleFileInfoList(mediaPlayerRepository: MediaPlayerRepository): GetSubtitleFileInfoList =
            GetSubtitleFileInfoList(mediaPlayerRepository::getSubtitleFileInfoList)
    }
}