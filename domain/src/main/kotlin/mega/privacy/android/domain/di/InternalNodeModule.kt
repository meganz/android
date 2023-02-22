package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.DefaultGetFolderTreeInfo
import mega.privacy.android.domain.usecase.DefaultGetNodeById
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeById
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions

/**
 * module to provide FileNode modules
 */
@Module
@DisableInstallInCheck
abstract class InternalNodeModule {

    /**
     * provides default GetFolderInfo
     */
    @Binds
    abstract fun bindGetFolderVersionInfoByHandle(implementation: DefaultGetFolderTreeInfo): GetFolderTreeInfo

    /**
     * Provide implementation for [GetNodeById]
     */
    @Binds
    abstract fun bindGetNodeById(implementation: DefaultGetNodeById): GetNodeById

    companion object {
        /**
         * provides [GetFileHistoryNumVersions]
         */
        @Provides
        fun providesGetFileHistoryNumVersions(nodeRepository: NodeRepository): GetFileHistoryNumVersions =
            GetFileHistoryNumVersions(nodeRepository::getNodeHistoryNumVersions)

        /**
         * provides [IsNodeInInbox]
         */
        @Provides
        fun provideIsNodeInInbox(nodeRepository: NodeRepository): IsNodeInInbox =
            IsNodeInInbox(nodeRepository::isNodeInInbox)
    }
}