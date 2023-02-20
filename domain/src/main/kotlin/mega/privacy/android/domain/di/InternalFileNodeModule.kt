package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.DefaultGetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions

/**
 * module to provide FileNode modules
 */
@Module
@DisableInstallInCheck
abstract class InternalFileNodeModule {

    /**
     * provides default GetFolderInfo
     */
    @Binds
    abstract fun bindGetFolderVersionInfoByHandle(implementation: DefaultGetFolderTreeInfo): GetFolderTreeInfo

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