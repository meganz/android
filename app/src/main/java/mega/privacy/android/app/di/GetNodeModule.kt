package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.CopyNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.GetUnverifiedOutgoingShares

/**
 * Get node module
 *
 * Provides use cases for manager activity and camera upload service
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class GetNodeModule {

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
    }
}
