package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.MediaPlayerRepository
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
import mega.privacy.android.domain.usecase.GetAudioNodes
import mega.privacy.android.domain.usecase.GetAudioNodesByEmail
import mega.privacy.android.domain.usecase.GetAudioNodesByParentHandle
import mega.privacy.android.domain.usecase.GetAudioNodesFromInShares
import mega.privacy.android.domain.usecase.GetAudioNodesFromOutShares
import mega.privacy.android.domain.usecase.GetAudioNodesFromPublicLinks
import mega.privacy.android.domain.usecase.GetAudiosByParentHandleFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandle
import mega.privacy.android.domain.usecase.GetNodesByHandles
import mega.privacy.android.domain.usecase.GetRootNode
import mega.privacy.android.domain.usecase.GetRootNodeFromMegaApiFolder
import mega.privacy.android.domain.usecase.GetRubbishNode
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
         * Provide implementation for [GetRubbishNode]
         */
        @Provides
        fun provideGetRubbishNode(mediaPlayerRepository: MediaPlayerRepository): GetRubbishNode =
            GetRubbishNode(mediaPlayerRepository::getRubbishNode)

        /**
         * Provide implementation for [GetRootNodeFromMegaApiFolder]
         */
        @Provides
        fun provideGetRootNodeFromMegaApiFolder(mediaPlayerRepository: MediaPlayerRepository): GetRootNodeFromMegaApiFolder =
            GetRootNodeFromMegaApiFolder(mediaPlayerRepository::getRootNodeFromMegaApiFolder)

        /**
         * Provide implementation for [GetUnTypedNodeByHandle]
         */
        @Provides
        fun provideGetUnTypedNodeByHandle(mediaPlayerRepository: MediaPlayerRepository): GetUnTypedNodeByHandle =
            GetUnTypedNodeByHandle(mediaPlayerRepository::getUnTypedNodeByHandle)

        /**
         * Provide implementation for [GetFileUrlByNodeHandle]
         */
        @Provides
        fun provideHttpServerGetLocalLink(mediaPlayerRepository: MediaPlayerRepository): GetFileUrlByNodeHandle =
            GetFileUrlByNodeHandle(mediaPlayerRepository::getFileUrlByNodeHandle)
    }
}