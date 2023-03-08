package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.rx3.await
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.CopyNode
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.UpgradeSecurity
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandle
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandleChangingName
import mega.privacy.android.domain.usecase.filenode.DefaultDeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle

/**
 * Get node module
 *
 * Provides use cases for managing nodes
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class GetNodeModule {


    /**
     * Provides the [DeleteNodeVersionsByHandle] implementation
     *
     * @param defaultDeleteNodeVersionsByHandle the default implementation to be provided
     * @return [DeleteNodeVersionsByHandle]
     */
    @Binds
    abstract fun provideDeleteNodeVersionsByHandle(defaultDeleteNodeVersionsByHandle: DefaultDeleteNodeVersionsByHandle): DeleteNodeVersionsByHandle

    companion object {

        /**
         * Provides the [CopyNode] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [CopyNode]
         */
        @Provides
        fun provideCopyNode(megaNodeRepository: MegaNodeRepository): CopyNode =
            CopyNode(megaNodeRepository::copyNode)

        /**
         * Provides the [CopyNodeByHandle] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [CopyNodeByHandle]
         */
        @Provides
        fun provideCopyNodeByHandle(megaNodeRepository: MegaNodeRepository): CopyNodeByHandle =
            CopyNodeByHandle { node, parent ->
                megaNodeRepository.copyNodeByHandle(node, parent, null)
            }

        /**
         * Provides the [CopyNodeByHandleChangingName] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [CopyNodeByHandleChangingName]
         */
        @Provides
        fun provideCopyNodeByHandleChangingName(megaNodeRepository: MegaNodeRepository): CopyNodeByHandleChangingName =
            CopyNodeByHandleChangingName(megaNodeRepository::copyNodeByHandle)


        /**
         * Provides the [GetChildrenNode] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [GetChildrenNode]
         */
        @Provides
        fun provideGetChildrenNode(megaNodeRepository: MegaNodeRepository): GetChildrenNode =
            GetChildrenNode(megaNodeRepository::getChildrenNode)

        /**
         * Provides the [GetNodeByHandle] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [GetNodeByHandle]
         */
        @Provides
        fun provideGetNodeByHandle(megaNodeRepository: MegaNodeRepository): GetNodeByHandle =
            GetNodeByHandle(megaNodeRepository::getNodeByHandle)

        /**
         * Provides [GetUnverifiedIncomingShares] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [GetUnverifiedIncomingShares]
         */
        @Provides
        fun provideGetUnVerifiedInComingShares(megaNodeRepository: MegaNodeRepository): GetUnverifiedIncomingShares =
            GetUnverifiedIncomingShares(megaNodeRepository::getUnverifiedIncomingShares)

        /**
         * Provides [GetUnverifiedOutgoingShares] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [GetUnverifiedOutgoingShares]
         */
        @Provides
        fun provideGetUnverifiedOutGoingShares(megaNodeRepository: MegaNodeRepository): GetUnverifiedOutgoingShares =
            GetUnverifiedOutgoingShares(megaNodeRepository::getUnverifiedOutgoingShares)

        /**
         * Provides [CheckNameCollision] implementation
         * @param checkNameCollisionUseCase [CheckNameCollisionUseCase] is the current implementation, not following the current architecture
         * @return [CheckNameCollision]
         */
        @Provides
        fun provideCheckNameCollision(checkNameCollisionUseCase: CheckNameCollisionUseCase): CheckNameCollision =
            CheckNameCollision { nodeHandle, parentNodeHandle, type ->
                checkNameCollisionUseCase.check(
                    nodeHandle.longValue,
                    parentNodeHandle.longValue,
                    type
                ).await()
            }

        /**
         * Provides [MoveNodeByHandle] implementation
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [MoveNodeByHandle]
         */
        @Provides
        fun provideMoveNodeByHandle(megaNodeRepository: MegaNodeRepository): MoveNodeByHandle =
            MoveNodeByHandle { node, parent ->
                megaNodeRepository.moveNodeByHandle(node, parent, null)
            }

        /**
         * Provides [MoveNodeToRubbishByHandle] implementation
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [MoveNodeToRubbishByHandle]
         */
        @Provides
        fun provideMoveNodeToRubbishByHandle(
            megaNodeRepository: MegaNodeRepository,
        ): MoveNodeToRubbishByHandle =
            MoveNodeToRubbishByHandle(megaNodeRepository::moveNodeToRubbishBinByHandle)

        /**
         * Provides [DeleteNodeByHandle] implementation
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [DeleteNodeByHandle]
         */
        @Provides
        fun provideDeleteNodeByHandle(
            megaNodeRepository: MegaNodeRepository,
        ): DeleteNodeByHandle =
            DeleteNodeByHandle(megaNodeRepository::deleteNodeByHandle)

        /**
         * Provides [CreateShareKey] implementation
         *
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [CreateShareKey]
         */
        @Provides
        fun provideCreateShareKey(megaNodeRepository: MegaNodeRepository): CreateShareKey =
            CreateShareKey(megaNodeRepository::createShareKey)

        /**
         * Provides [UpgradeSecurity] implementation
         * @param megaNodeRepository [MegaNodeRepository]
         * @return [UpgradeSecurity]
         */
        @Provides
        fun provideUpgradeSecurity(megaNodeRepository: MegaNodeRepository): UpgradeSecurity =
            UpgradeSecurity(megaNodeRepository::upgradeSecurity)
    }
}
