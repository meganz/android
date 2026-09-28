package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.shares.DefaultGetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.DefaultSetOutgoingPermissions
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.SetOutgoingPermissions
import mega.privacy.android.domain.usecase.shares.StopSharingNode

/**
 * Provide use-cases related to share nodes
 */
@Module
@DisableInstallInCheck
abstract class InternalSharesModule {

    /**
     * Provides [GetContactItemFromInShareFolder] implementation.
     */
    @Binds
    abstract fun bindGetContactItemFromInShareFolder(useCase: DefaultGetContactItemFromInShareFolder): GetContactItemFromInShareFolder

    /**
     * Provides [SetOutgoingPermissions] implementation.
     */
    @Binds
    abstract fun bindDefaultSetShareAccess(useCase: DefaultSetOutgoingPermissions): SetOutgoingPermissions

    companion object {
        /**
         * Provides [GetNodeAccessPermission] use-case
         */
        @Provides
        fun provideGetNodeAccessPermission(repository: NodeRepository) =
            GetNodeAccessPermission(repository::getNodeAccessPermission)

        /**
         * Provides [StopSharingNode] use-case
         */
        @Provides
        fun provideStopSharingNode(repository: NodeRepository) =
            StopSharingNode(repository::stopSharingNode)
    }
}