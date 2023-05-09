package mega.privacy.android.app.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.rx3.await
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.data.repository.MegaNodeRepository
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
         * Provides [CheckNameCollision] implementation
         * @param checkNameCollisionUseCase [CheckNameCollisionUseCase] is the current implementation, not following the current architecture
         * @return [CheckNameCollision]
         */
        @Provides
        fun provideCheckNameCollision(
            checkNameCollisionUseCase: CheckNameCollisionUseCase,
            @ApplicationContext context: Context,
        ): CheckNameCollision =
            CheckNameCollision { nodeHandle, parentNodeHandle, type ->
                checkNameCollisionUseCase.check(
                    nodeHandle.longValue,
                    parentNodeHandle.longValue,
                    type,
                    context,
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
    }
}
